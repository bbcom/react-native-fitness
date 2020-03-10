package com.ovalmoney.fitness.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.fitness.data.Subscription;
import com.ovalmoney.fitness.permission.Request;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.ovalmoney.fitness.permission.Permission.ACTIVITY;
import static com.ovalmoney.fitness.permission.Permission.CALORIES;
import static com.ovalmoney.fitness.permission.Permission.DISTANCE;
import static com.ovalmoney.fitness.permission.Permission.STEP;
import static com.ovalmoney.fitness.permission.Permission.HEART_RATE;
import static com.ovalmoney.fitness.permission.Permission.WEIGHT;

public class Manager implements ActivityEventListener {

    private final static int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 111;
    private final static int GOOGLE_PLAY_SERVICE_ERROR_DIALOG = 2404;
    private final static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());

    private Promise promise;

    private static boolean isGooglePlayServicesAvailable(final Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if(status != ConnectionResult.SUCCESS) {
            if(googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, GOOGLE_PLAY_SERVICE_ERROR_DIALOG).show();
            }
            return false;
        }
        return true;
    }

    protected FitnessOptions.Builder addPermissionToFitnessOptions(final FitnessOptions.Builder fitnessOptions, final ArrayList<Request> permissions){
        int length = permissions.size();
        for(int i = 0; i < length; i++){
            Request currentRequest = permissions.get(i);
            switch(currentRequest.permissionKind){
                case STEP:
                    fitnessOptions
                            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, currentRequest.permissionAccess)
                            .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, currentRequest.permissionAccess);
                    break;
                case DISTANCE:
                    fitnessOptions.addDataType(DataType.TYPE_DISTANCE_DELTA, currentRequest.permissionAccess);
                    break;
                case CALORIES:
                    fitnessOptions.addDataType(DataType.TYPE_CALORIES_EXPENDED, currentRequest.permissionAccess);
                    break;
                case ACTIVITY:
                    fitnessOptions.addDataType(DataType.TYPE_ACTIVITY_SEGMENT, currentRequest.permissionAccess);
                    break;
                case HEART_RATE:
                    fitnessOptions.addDataType(DataType.TYPE_HEART_RATE_BPM, currentRequest.permissionAccess);
                    break;
				case WEIGHT:
                    fitnessOptions.addDataType(DataType.TYPE_WEIGHT, currentRequest.permissionAccess);
                    break;
                default:
                    break;
            }
        }

        return fitnessOptions;
    }

    public boolean isAuthorized(final Activity activity, final ArrayList<Request> permissions){
        if(isGooglePlayServicesAvailable(activity)) {
            FitnessOptions fitnessOptions = addPermissionToFitnessOptions(FitnessOptions.builder(), permissions)
                    .build();
            return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(activity), fitnessOptions);
        }
        return false;
    }

    public void requestPermissions(@NonNull Activity currentActivity, final ArrayList<Request> permissions, Promise promise) {
        try {
            this.promise = promise;
            FitnessOptions fitnessOptions = addPermissionToFitnessOptions(FitnessOptions.builder(), permissions)
                    .build();
            GoogleSignIn.requestPermissions(
                    currentActivity,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(currentActivity.getApplicationContext()),
                    fitnessOptions);
        }catch(Exception e){
            Log.e(getClass().getName(), e.getMessage());
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            promise.resolve(true);
        }
        if (resultCode == Activity.RESULT_CANCELED && requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            promise.resolve(false);
        }
    }

    @Override
    public void onNewIntent(Intent intent) { }

    public void subscribeToActivity(Context context, final Promise promise){
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if(account == null){
            promise.resolve(false);
            return;
        }
        Fitness.getRecordingClient(context, account)
                .subscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        promise.resolve(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        promise.resolve(false);
                    }
                });

    }

    public void subscribeToSteps(Context context, final Promise promise){
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if(account == null){
            promise.resolve(false);
            return;
        }
        Fitness.getRecordingClient(context, account)
                .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        promise.resolve(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        promise.resolve(false);
                    }
                });
    }

    public void getSteps(Context context, double startDate, double endDate, String customInterval, final Promise promise){
        DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("estimated_steps")
                .setAppPackageName("com.google.android.gms")
                .build();

        TimeUnit interval;
        if(customInterval == "hour"){
            interval = TimeUnit.HOURS;
        }else{
            interval = TimeUnit.DAYS;
        }

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(ESTIMATED_STEP_DELTAS,    DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, interval)
                .setTimeRange((long) startDate, (long) endDate, TimeUnit.MILLISECONDS)
                .build();

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        if (dataReadResponse.getBuckets().size() > 0) {
                            WritableArray steps = Arguments.createArray();
                            for (Bucket bucket : dataReadResponse.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    processStep(dataSet, steps);
                                }
                            }
                            promise.resolve(steps);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        promise.reject(e);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<DataReadResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<DataReadResponse> task) {
                    }
                });
    }

    public void getDistance(Context context, double startDate, double endDate, String customInterval, final Promise promise) {
        TimeUnit interval;
        if(customInterval == "hour"){
            interval = TimeUnit.HOURS;
        }else{
            interval = TimeUnit.DAYS;
        }

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, interval)
                .setTimeRange((long) startDate, (long) endDate, TimeUnit.MILLISECONDS)
                .build();

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        if (dataReadResponse.getBuckets().size() > 0) {
                            WritableArray distances = Arguments.createArray();
                            for (Bucket bucket : dataReadResponse.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    processDistance(dataSet, distances);
                                }
                            }
                            promise.resolve(distances);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        promise.reject(e);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<DataReadResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<DataReadResponse> task) {
                    }
                });
    }

    public void getCalories(Context context, double startDate, double endDate, String customInterval, final Promise promise) {
        TimeUnit interval;
        if(customInterval == "hour"){
            interval = TimeUnit.HOURS;
        }else{
            interval = TimeUnit.DAYS;
        }
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByTime(1, interval)
                .setTimeRange((long) startDate, (long) endDate, TimeUnit.MILLISECONDS)
                .build();

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        if (dataReadResponse.getBuckets().size() > 0) {
                            WritableArray calories = Arguments.createArray();
                            for (Bucket bucket : dataReadResponse.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    processDistance(dataSet, calories);
                                }
                            }
                            promise.resolve(calories);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        promise.reject(e);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<DataReadResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<DataReadResponse> task) {
                    }
                });
    }

     public void getHeartRate(Context context, double startDate, double endDate, String customInterval,final Promise promise) {
        TimeUnit interval;
        if(customInterval == "hour"){
            interval = TimeUnit.HOURS;
        }else{
            interval = TimeUnit.DAYS;
        }

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_HEART_RATE_BPM, DataType.AGGREGATE_HEART_RATE_SUMMARY)
                .bucketByTime(1, interval)
                .setTimeRange((long) startDate, (long) endDate, TimeUnit.MILLISECONDS)
                .build();

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        if (dataReadResponse.getBuckets().size() > 0) {
                            WritableArray heartRates = Arguments.createArray();
                            for (Bucket bucket : dataReadResponse.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    processHeartRate(dataSet, heartRates);
                                }
                            }
                            promise.resolve(heartRates);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        promise.reject(e);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<DataReadResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<DataReadResponse> task) {
                    }
                });
    }

    // Get the most recent weight
    public void getWeight(Context context, final Promise promise) {
        Calendar calendar = Calendar.getInstance();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(DataType.TYPE_WEIGHT)
                .setTimeRange(1, calendar.getTimeInMillis(), TimeUnit.MILLISECONDS)
                .setLimit(1)
                .build();

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        if (!dataReadResponse.getDataSet(DataType.TYPE_WEIGHT).isEmpty()) {
                            DataPoint result = dataReadResponse.getDataSet(DataType.TYPE_WEIGHT).getDataPoints().get(0);
                            Double kilograms = (double) result.getValue(Field.FIELD_WEIGHT).asFloat();

                            WritableMap weightMap = Arguments.createMap();
                            weightMap.putDouble("kilograms",kilograms);
                            weightMap.putDouble("grams",(kilograms * 1000));
                            weightMap.putDouble("pounds", (kilograms * 2.20462262185));
                            weightMap.putDouble("startDate", result.getStartTime(TimeUnit.MILLISECONDS));
                            weightMap.putDouble("endDate", result.getEndTime(TimeUnit.MILLISECONDS));
                            promise.resolve(weightMap);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        promise.reject(e);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<DataReadResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<DataReadResponse> task) {
                    }
                });
    }

    public void saveActivity(Context context, String activity, double startDate, double endDate, final Promise promise) {
        DataSource dataSource = new DataSource.Builder()
            .setAppPackageName(context)
            .setDataType(DataType.TYPE_ACTIVITY_SEGMENT)
            .setStreamName("save_activity_" + activity.replace(".","_"))
            .setType(DataSource.TYPE_RAW)
            .build();

        DataPoint dataPoint = DataPoint.builder(dataSource)
                        .setTimeInterval((long) startDate, (long) endDate, TimeUnit.MILLISECONDS)
                        .setActivityField(Field.FIELD_ACTIVITY, activity)
                        .build();

        DataSet dataSet = DataSet.builder(dataSource).add(dataPoint).build();

        Fitness.getHistoryClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .insertData(dataSet)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        promise.reject(e);
                    }
                })
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // Data has been inserted and can be read.
                                    promise.resolve(true);
                                } else {
                                    System.out.println("There was a problem inserting the DataSet." + task.getException());
                                    promise.reject("SaveActivityError",task.getException());
                                }
                            }
                        });
    }

    private void processStep(DataSet dataSet, WritableArray map) {

        WritableMap stepMap = Arguments.createMap();

        for (DataPoint dp : dataSet.getDataPoints()) {
            for(Field field : dp.getDataType().getFields()) {
                stepMap.putString("startDate", dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
                stepMap.putString("endDate", dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                stepMap.putDouble("quantity", dp.getValue(field).asInt());
                map.pushMap(stepMap);
            }
        }
    }

    private void processDistance(DataSet dataSet, WritableArray map) {

        WritableMap distanceMap = Arguments.createMap();

        for (DataPoint dp : dataSet.getDataPoints()) {
            for(Field field : dp.getDataType().getFields()) {
                distanceMap.putString("startDate", dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
                distanceMap.putString("endDate", dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                distanceMap.putDouble("quantity", dp.getValue(field).asFloat());
                map.pushMap(distanceMap);
            }
        }
    }

    private void processCalories(DataSet dataSet, WritableArray map) {

        WritableMap caloryMap = Arguments.createMap();

        for (DataPoint dp : dataSet.getDataPoints()) {
            for(Field field : dp.getDataType().getFields()) {
                caloryMap.putString("startDate", dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
                caloryMap.putString("endDate", dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                caloryMap.putDouble("quantity", dp.getValue(field).asFloat());
                map.pushMap(caloryMap);
            }
        }
    }

    private void processHeartRate(DataSet dataSet, WritableArray map) {

        WritableMap heartRateMap = Arguments.createMap();

        for (DataPoint dp : dataSet.getDataPoints()) {
            for(Field field : dp.getDataType().getFields()) {
                heartRateMap.putString("startDate", dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
                heartRateMap.putString("endDate", dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
                heartRateMap.putDouble("quantity", dp.getValue(field).asFloat());
                map.pushMap(heartRateMap);
            }
        }
    }
}
