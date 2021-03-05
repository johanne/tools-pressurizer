#include <jni.h>
#include <string>
#include <unistd.h>
#include <pthread.h>
#include <sys/mman.h>
#include <android/log.h>

#define LOG_TAG "JNI_MemoryPressureNative"
#define ALOG(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define INITIAL_MULTIPLIER (getpagesize() / 1024) // want 1kb chunks
#define CHUNKS (10 * getpagesize() * 1024 / INITIAL_MULTIPLIER) // 10 MB chunks
#define MB (1024 * 1024)
#define ALLOCATION_MAX 2000 // 2000 * 10MB = 20GB

pthread_t monitorThread = -1;
bool exitMonitorThread = false;
JavaVM *jvm = 0;
char *memalloc[ALLOCATION_MAX];
int counter;

long getFreeMemory() {
    FILE *meminfo = fopen("/proc/meminfo", "r");
    if (meminfo == 0) {
        ALOG("Cannot open /proc/meminfo");
        return -1;
    }

    // estimate free mem instead
    // need to get MemFree, Active(file), Inactive(file), SReclaimable
    const char *memFree = "MemFree";
    const char *activeFile = "Active(file)";
    const char *inactiveFile = "Inactive(file)";
    const char *sReclaimable = "SReclaimable";
    // skip low watermarks because we don't have access to /proc/zoneinfo

    int numItemsToFind = 3;
    char * line = nullptr;
    size_t len = 0;
    long freeMemory = 0;
    while(numItemsToFind > 0 && getline(&line, &len, meminfo) != -1) {
        if (strstr(line, memFree) || strstr(line, activeFile) || strstr(line, inactiveFile)
            || strstr(line, sReclaimable)) {
            // always assume second is the free memory
            ALOG("Found line: %s", line);
            strtok(line, " ");
            char *memoryValue = strtok(nullptr, " ");
            ALOG("MemoryValue line: %s", memoryValue);
            long toAdd = (strtol(memoryValue, nullptr, 10) * 1024);
            // overshoot this since we can't get to /proc/zoneinfo
            if (strstr(line, sReclaimable)) {
                toAdd /= 2;
            }
            freeMemory += toAdd;
            numItemsToFind--;
        }
    }

    fclose(meminfo);
    if (line)
        free(line);
    return freeMemory;
}

long getTotalMemory() {
    long pages = sysconf(_SC_PHYS_PAGES);
    long page_size = getpagesize();
    return pages * page_size;
}

long getUsedMemory() {
    if (getFreeMemory() == -1) {
        return -1;
    }
    return getTotalMemory() - getFreeMemory();
}

void lockMemory(long memoryToUse) {
    int iterations = memoryToUse / CHUNKS;
    if (iterations > 0) {
        ALOG("Memory to capture: %ld MB", memoryToUse / MB);
        ALOG("Iterations to capture all memory %d", iterations);
        for (int i = 0; i < iterations && counter < ALLOCATION_MAX; counter++, i++) {
            memalloc[counter] = (char *) malloc(CHUNKS);
            memset(memalloc[counter], 'a', CHUNKS);
            mlock(memalloc[counter], CHUNKS);
        }
        ALOG("Current allocation count: %d", counter);
    }
}

void *memoryMonitorPercent(void *args) {
    JNIEnv *env;
    (*jvm).AttachCurrentThread(&env, nullptr);
    float percent = (*(float *) args);
    ALOG("Page Size Raw: %d", getpagesize());
    ALOG("Chunks in MB: %d", CHUNKS / MB);
    ALOG("Percent of total memory to capture: %f", percent);
    while (!exitMonitorThread) {
        long totalToUseUp = (long) (percent * getTotalMemory());
        ALOG("Total Memory To Use Up in MB: %ld", totalToUseUp / MB);
        long memoryToUse = totalToUseUp - getUsedMemory();
        lockMemory(memoryToUse);
        if (memoryToUse / CHUNKS > 0) {
            sleep(1); // sleep for a shorter time so that we can quickly allocate memory
        } else {
            sleep(5); // so that we don't use up too much CPU
        }
    }
    free(args);
    (*jvm).DetachCurrentThread();
    pthread_exit(NULL);
}




extern "C"
JNIEXPORT jlong JNICALL
Java_com_jdemetria_tools_pressuriser_MemoryPressureNative_00024Companion_getFreeMemory(JNIEnv *env,
                                                                                       jobject thiz) {
    return getFreeMemory();
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_jdemetria_tools_pressuriser_MemoryPressureNative_00024Companion_getUsedMemory(JNIEnv *env,
                                                                                       jobject thiz) {
    return getUsedMemory();
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_jdemetria_tools_pressuriser_MemoryPressureNative_00024Companion_getTotalMemory(JNIEnv *env,
                                                                                        jobject thiz) {
    return getTotalMemory();
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_jdemetria_tools_pressuriser_MemoryPressureNative_00024Companion_lockPercentOfMemory(
        JNIEnv *env, jobject thiz, jfloat percent) {
    if (monitorThread != -1) {
        ALOG("Thread still created.");
        return 1;
    }
    (*env).GetJavaVM(&jvm);
    exitMonitorThread = false;
    ALOG("Creating monitor thread.");
    float *percentage;
    percentage = (float *) malloc(sizeof(float));
    memcpy(percentage, &percent, sizeof(float));
    pthread_create(&monitorThread, NULL, &memoryMonitorPercent, percentage);
    return 0;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_jdemetria_tools_pressuriser_MemoryPressureNative_00024Companion_freeLockedMemory(
        JNIEnv *env, jobject thiz) {
    if (monitorThread != -1) {
        ALOG("Freeing memory.");
        exitMonitorThread = true;
        pthread_join(monitorThread, NULL);
        monitorThread = -1;
        ALOG("pthread_join success.");
        ALOG("Freeing malloc'd memory[array size]: %d", counter);
    }
    if (counter <= 0) {
        ALOG("Nothing to free.");
        counter = 0;
        return false;
    }
    while (counter-- > 0) {
        munlock(memalloc[counter], CHUNKS);
        free(memalloc[counter]);
    }
    counter = 0;
    ALOG("Successfully freed all memory.");

    return true;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_jdemetria_tools_pressuriser_MemoryPressureNative_00024Companion_lockMemoryInMB(JNIEnv *env,
                                                                                        jobject thiz,
                                                                                        jlong mem_in_mb) {
    mem_in_mb *= MB;
    lockMemory(mem_in_mb);
    return 0;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_jdemetria_tools_pressuriser_MemoryPressureNative_00024Companion_leaveMemoryInMB(
        JNIEnv *env, jobject thiz, jlong mem_in_mb) {
    mem_in_mb *= MB;
    long freeMemory = getFreeMemory();
    ALOG("Memory to leave %ld", mem_in_mb);
    ALOG("Free Memory %ld", freeMemory);
    ALOG("Remaining Memory %lld", freeMemory - mem_in_mb);

    if (freeMemory < mem_in_mb) {
        return 0;
    }
    lockMemory(freeMemory - mem_in_mb);
    return freeMemory - mem_in_mb;
}