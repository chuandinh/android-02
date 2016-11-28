// IMyAidlInterface.aidl
package com.example.dinh.alphafitness;

// Declare any non-default types here with import statements

interface IMyAidlInterface {
    boolean isServiceStarted();
    boolean isRunning();
    int getStepCount();
    long getCurrentWorkoutId();
    long getStartTime();
    void startWorkout();
    void stopWorkout();
}
