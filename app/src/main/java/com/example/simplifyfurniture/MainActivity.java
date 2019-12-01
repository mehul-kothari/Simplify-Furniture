package com.example.simplifyfurniture;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private Uri selectedObject;
    private Button viewArButton;
    private DrawerLayout mDrawer;
    private ListView mDrawerListView;
    private FirebaseAuth firebaseAuth;
    private FloatingActionButton toggleRecording;
    private VideoRecorder videoRecorder;
    private ScrollView scrollView;
    private LinearLayout verticalLinearLayout;
    private Button logout;
    private View dialogView;
    final long ONE_MEGABYTE = 1024 * 1024;
    private AlertDialog alertDialog;


    /**
     * Permission to write to external storage
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

    }

    /**
     * Set up video recording using VideoRecorder.Java
     */
    private void setupVideoRecorder() {
        toggleRecording.setOnClickListener(v -> {
            if (videoRecorder == null) {
                videoRecorder = new VideoRecorder();
                videoRecorder.setSceneView(arFragment.getArSceneView());
                int orientation = getResources().getConfiguration().orientation;
                videoRecorder.setVideoQuality(CamcorderProfile.QUALITY_HIGH, orientation);
            }

            boolean isRecording = videoRecorder.onToggleRecord();
            if (isRecording) {
                Toast toast =
                        Toast.makeText(this, "Video recording started", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else {
                Toast toast =
                        Toast.makeText(this, "Video recording stopped", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });

    }

    /**
     * Setting up Navigation Bar - contains logout and populate database for now
     * Need to comment this out
     */
//    private void setupNavBar() {
//        mDrawerListView.setOnItemClickListener(new ListView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                String selectedItem = (String) parent.getItemAtPosition(position);
//                if (selectedItem.equals("Logout")) {
//                    firebaseAuth.signOut();
//                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//                    if (user != null) {
//                        Toast.makeText(MainActivity.this, "User still logged in", Toast.LENGTH_LONG);
//                    } else {
//                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
//                        finish();
//                    }
//                } else if (selectedItem.equals("Populate Database")) {
//                    Intent intent = new Intent(MainActivity.this, PopulateDatabaseActivity.class);
//                    startActivity(intent);
//                }
//                mDrawer.closeDrawer(GravityCompat.START);
//            }
//        });
//    }

    //LogOut Button
    private void setUpLogout() {
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                firebaseAuth.signOut();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    Toast.makeText(MainActivity.this, "User still logged in", Toast.LENGTH_LONG);
                } else {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        });
    }

    /**
     * Setting up AR Fragment
     */
    private void setupArFragment() {
        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            Anchor anchor = hitResult.createAnchor();
            if (selectedObject != null) {
                ModelRenderable.builder()
                        .setSource(this, selectedObject)
                        .build()
                        .thenAccept(modelRenderable -> addModelToScene(anchor, modelRenderable))
                        .exceptionally(
                                throwable -> {
                                    Toast toast =
                                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                    return null;
                                });
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_drawer);
        firebaseAuth = FirebaseAuth.getInstance();
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        mDrawerListView = (ListView) findViewById(R.id.left_drawer);
        logout = findViewById(R.id.logout);
        toggleRecording = findViewById(R.id.toggle_recording);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        scrollView = findViewById(R.id.vertical_scroll_view);
        verticalLinearLayout = findViewById(R.id.vertical_linear_layout);

        setupVideoRecorder();

//        setupNavBar();
        setUpLogout();

        setupArFragment();
        
        initializeGallery();


    }

    private void addModelToScene(Anchor anchor, ModelRenderable modelRenderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(modelRenderable);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        transformableNode.select();
    }

    private void createAlertDialog() {

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        dialogView = layoutInflater.inflate(R.layout.layout_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);
        alertDialogBuilder.setView(dialogView);
        alertDialog = alertDialogBuilder.create();

    }

    private void initializeGallery() {

        LinearLayout.LayoutParams viewParamsCenter = new LinearLayout.LayoutParams(150, 150);
        viewParamsCenter.setMargins(10, 10, 10, 10);

        createAlertDialog();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        ValueEventListener eventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> list = new ArrayList<>();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String fType = ds.getKey();
                    ImageView imageView;
                    HorizontalScrollView horizontalScrollView = new HorizontalScrollView(MainActivity.this);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    horizontalScrollView.setLayoutParams(layoutParams);
                    LinearLayout linearLayout = new LinearLayout(MainActivity.this);
                    LinearLayout.LayoutParams linearParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    linearLayout.setOrientation(LinearLayout.HORIZONTAL);
                    linearLayout.setLayoutParams(linearParams);
                    horizontalScrollView.addView(linearLayout);
                    verticalLinearLayout.addView(horizontalScrollView);
//                    LinearLayout linearLayout = findViewById(linearLayoutIds.get(fType.toLowerCase()));
                    for (DataSnapshot ds1 : ds.getChildren()) {
                        Furniture furniture = null;
                        try {
                            furniture = ds1.getValue(Furniture.class);
                        } catch (Exception e) {
                            break;
                        }
                        imageView = new ImageView(MainActivity.this);
                        imageView.setLayoutParams(viewParamsCenter);
                        //imageView.setId(Integer.valueOf(ds1.getKey()));
                        //Get from Storage
                        imageView.setBackgroundResource(R.drawable.chair1);
                        StorageReference storageReference =
                                storage.getReferenceFromUrl(furniture.getUrl());
                        ImageView finalImageView = imageView;
                        storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new
                                                                                             OnSuccessListener<byte[]>() {
                                                                                                 @Override
                                                                                                 public void onSuccess(byte[] bytes) {
                                                                                                     Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                                                                                     finalImageView.setImageBitmap(bitmap);
                                                                                                 }
                                                                                             }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle any errors
                            }
                        });
                        linearLayout.addView(finalImageView);
                        finalImageView.setOnClickListener(v -> {
                            System.out.println(ds1.getKey());
                            alertDialog.create();
                            alertDialog.show();


                        });

                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        rootRef.addListenerForSingleValueEvent(eventListener);

        viewArButton = dialogView.findViewById(R.id.viewar);
        viewArButton.setOnClickListener(v1 -> {
            Toast.makeText(this, "Hello", Toast.LENGTH_LONG).show();
            selectedObject = Uri.parse("chair3.sfb");
            alertDialog.dismiss();
        });

    }
}
