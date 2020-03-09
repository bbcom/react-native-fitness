package com.ovalmoney.fitness.permission;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;


    @Retention(SOURCE)
    @IntDef({
            Permission.STEP,
            Permission.DISTANCE,
            Permission.ACTIVITY,
            Permission.CALORIES,
            Permission.WEIGHT,
    })

    public @interface Permission {
        int STEP = 0;
        int DISTANCE = 1;
        int ACTIVITY = 2;
        int CALORIES = 3;
        int WEIGHT = 4;
    }

