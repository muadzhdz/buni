package core

/*
#cgo CFLAGS: -I/usr/lib/jvm/default/include -I/usr/lib/jvm/default/include/linux
#include <jni.h>
#include <stdlib.h>

static const char* get_utf_chars(JNIEnv* env, jstring s) {
    if (!s) return NULL;
    return (*env)->GetStringUTFChars(env, s, NULL);
}

static void release_utf_chars(JNIEnv* env, jstring s, const char* chars) {
    if (s && chars) {
        (*env)->ReleaseStringUTFChars(env, s, chars);
    }
}

static jstring new_utf_string(JNIEnv* env, const char* s) {
    if (!s) return NULL;
    return (*env)->NewStringUTF(env, s);
}
*/
import "C"
import "unsafe"

//export Java_dev_buni_core_Bridge_startShield
func Java_dev_buni_core_Bridge_startShield(env *C.JNIEnv, thiz C.jobject, fd C.jint, blocklistData C.jstring, dohEndpoint C.jstring) C.jstring {
	cBlocklist := C.get_utf_chars(env, blocklistData)
	var blocklistStr string
	if cBlocklist != nil {
		blocklistStr = C.GoString(cBlocklist)
		C.release_utf_chars(env, blocklistData, cBlocklist)
	}

	cDoh := C.get_utf_chars(env, dohEndpoint)
	var dohStr string
	if cDoh != nil {
		dohStr = C.GoString(cDoh)
		C.release_utf_chars(env, dohEndpoint, cDoh)
	}

	res := StartShield(int(fd), blocklistStr, dohStr)
	cRes := C.CString(res)
	defer C.free(unsafe.Pointer(cRes))

	return C.new_utf_string(env, cRes)
}

//export Java_dev_buni_core_Bridge_stopShield
func Java_dev_buni_core_Bridge_stopShield(env *C.JNIEnv, thiz C.jobject) {
	StopShield()
}

//export Java_dev_buni_core_Bridge_getStats
func Java_dev_buni_core_Bridge_getStats(env *C.JNIEnv, thiz C.jobject) C.jstring {
	stats := GetStats()
	cStats := C.CString(stats)
	defer C.free(unsafe.Pointer(cStats))

	return C.new_utf_string(env, cStats)
}

//export Java_dev_buni_core_Bridge_pollLog
func Java_dev_buni_core_Bridge_pollLog(env *C.JNIEnv, thiz C.jobject) C.jstring {
	logLine := PollLog()
	cLog := C.CString(logLine)
	defer C.free(unsafe.Pointer(cLog))

	return C.new_utf_string(env, cLog)
}
