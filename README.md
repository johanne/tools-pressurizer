# tools-pressurizer

### Use at your own risk.

This project is for applying artificial pressure to Android devices. At the moment, the following proof-of-concept has been made to work up to Android 11 devices:
* Forcing a single-memory allocation.
* Freeing memory allocations.
* Running a pthread to constantly allocate memory as the Garbage Collector cleans up other processes.
* Running as a service that receives broadcasts on whether or not to force-allocate or free up memory.

This **does not** handle the following situations:
* The system has reserved pages, which show up as used.
* The system decides to kill the broadcaster receiver due to extreme memory pressure. 
  * From personal testing, the system frees up the pthread allocated space. The broadcast receiver is then restarted once a new broadcast is received.
