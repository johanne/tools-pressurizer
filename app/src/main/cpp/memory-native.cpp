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
    long pages = sysconf(_SC_AVPHYS_PAGES);
    long page_size = getpagesize();
    return pages * page_size;
}

long getTotalMemory() {
    long pages = sysconf(_SC_PHYS_PAGES);
    long page_size = getpagesize();
    return pages * page_size;
}

long getUsedMemory() {
    return getTotalMemory() - getFreeMemory();
}

void *memoryMonitorPercent(void *args) {
    JNIEnv *env;
    (*jvm).AttachCurrentThread(&env, NULL);
    float percent = (*(float *) args);
    ALOG("Page Size Raw: %d", getpagesize());
    ALOG("Chunks in MB: %d", CHUNKS / MB);
    ALOG("Percent of total memory to capture: %f", percent);
    while (!exitMonitorThread) {
        long totalToUseUp = (long) (percent * getTotalMemory());
        long memoryToUse = totalToUseUp - getUsedMemory();
        int iterations = memoryToUse / CHUNKS;
        if (iterations > 0) {
            ALOG("Total Memory To Use Up in MB: %ld", totalToUseUp / MB);
            ALOG("Memory to capture: %ld", memoryToUse / MB);
            ALOG("Iterations to capture all memory %d", iterations);
            for (int i = 0; i < iterations && counter < ALLOCATION_MAX; counter++, i++) {
                memalloc[counter] = (char *) malloc(CHUNKS);
                memset(memalloc[counter], 'a', CHUNKS);
                mlock(memalloc[counter], CHUNKS);
            }
            ALOG("Current allocation count: %d", counter);
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
    return monitorThread;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_jdemetria_tools_pressuriser_MemoryPressureNative_00024Companion_freeLockedMemory(
        JNIEnv *env, jobject thiz) {
    if (monitorThread == -1) {
        ALOG("Nothing to free.");
        return false;
    }
    ALOG("Freeing memory.");
    exitMonitorThread = true;
    pthread_join(monitorThread, NULL);
    monitorThread = -1;
    ALOG("pthread_join success.");
    ALOG("Freeing malloc'd memory[array size]: %d", counter);

    while (counter-- > 0) {
        munlock(memalloc[counter], CHUNKS);
        free(memalloc[counter]);
    }
    counter = 0;
    ALOG("Successfully freed all memory.");

    return true;
}
