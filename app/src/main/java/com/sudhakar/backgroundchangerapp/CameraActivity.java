/* Copyright 2017 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.sudhakar.backgroundchangerapp;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.opencv.android.OpenCVLoader;


/**
 * Main {@code Activity} class for the Camera app.
 */
public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Fragment fragment=Camera2BasicFragment.newInstance();

        switchFragment(fragment ,false);

        System.loadLibrary("opencv_java4");




        if (!OpenCVLoader.initDebug())
            Log.e("OpenCv", "Unable to load OpenCV");
        else
            Log.d("OpenCv", "OpenCV loaded");

    }

    @Override
    protected void onStart() {
        super.onStart();

//        AppLogger.println(" >>>>>>>>> cam >>>>>>>> onStart ");
    }

    @Override
    protected void onRestart() {
        super.onRestart();

//        AppLogger.println(" >>>>>>>>> cam >>>>>>>> onRestart ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        AppLogger.println(" >>>>>>>>> cam >>>>>>>> onDestroy ");

    }

    @Override
    protected void onResume() {
        super.onResume();

//        AppLogger.println(" >>>>>>>>> cam >>>>>>>> onResume ");
    }

    @Override
    protected void onPause() {
        super.onPause();

//        AppLogger.println(" >>>>>>>>> cam >>>>>>>> onPause ");
    }

    @Override
    protected void onStop() {
        super.onStop();

//        AppLogger.println(" >>>>>>>>> cam >>>>>>>> onStop ");
    }

    public void switchFragment(Fragment fragment, boolean isAnimation) {


        if (isFragmentExist(fragment)) {

            try {

                getSupportFragmentManager().popBackStackImmediate(fragment.getClass().getName(), 1);

            } catch (IllegalStateException e) {

                e.printStackTrace();
            }
        }

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);




        fragmentTransaction.addToBackStack(fragment.getClass().getName());
        fragmentTransaction.replace(R.id.container, fragment, fragment.getClass().getName());

        try {

            fragmentTransaction.commit();

        } catch (IllegalStateException e2) {
            e2.printStackTrace();
        }
    }


    private boolean isFragmentExist(Fragment fragment) {

        if (fragment == null) return false;

        return getSupportFragmentManager().findFragmentByTag(fragment.getClass().getName()) != null;

    }



}
