package com.example.acute2.record;

/**
 * Created by cc on 2016/10/13.
 */

import android.os.Process;

import androidx.annotation.IntDef;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

//已经升级为android.x

/**
 * Allowed values for thread priority
 */
@IntDef(flag = true, value = {
        Process.THREAD_PRIORITY_BACKGROUND,
        Process.THREAD_PRIORITY_AUDIO,
        Process.THREAD_PRIORITY_DEFAULT,
        Process.THREAD_PRIORITY_DISPLAY,
        Process.THREAD_PRIORITY_URGENT_AUDIO,
        Process.THREAD_PRIORITY_URGENT_DISPLAY,
        Process.THREAD_PRIORITY_FOREGROUND,
        Process.THREAD_PRIORITY_LOWEST
})
@Retention(RetentionPolicy.SOURCE)
@Documented
@interface ProcessPriority {
}
