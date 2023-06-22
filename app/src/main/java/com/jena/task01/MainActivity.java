package com.jena.task01;


import android.Manifest;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int PERMISSION_REQUEST_CODE = 3;

    private EditText editTextName, editTextSurname;
    private RadioGroup radioGroupGender;
    private ImageView imageViewSelectedImage;

    private Bitmap bitmap;
    private Uri selectedImageUri;
    private FirebaseFirestore db;

    private String TAG = "LogTask01";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        // TEXTs
        editTextName = findViewById(R.id.editTextName);
        editTextSurname = findViewById(R.id.editTextSurname);
        radioGroupGender = findViewById(R.id.radioGroupGender);
        imageViewSelectedImage = findViewById(R.id.imageViewSelectedImage);

        // buttonSelectImage
        Button buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        // buttonSubmit
        Button buttonSubmit = findViewById(R.id.buttonSubmit);
        buttonSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFormData();
            }
        });

        // getButton
        Button stopWorker = findViewById(R.id.stopWorker);
        stopWorker.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
                WorkManager.getInstance(getApplicationContext()).cancelAllWork();
           }
        });

        // initialise the worker
//        initWorker();

    }

    private void initWorker() {
        // Create a constraints object that requires a network connection
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Create a one-time work request with the constraints
        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(MyWorker.class)
                        .setConstraints(constraints)
                        .build();

//        // Create a periodic work request to trigger the work every 1 minute
//        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
//                MyWorker.class,
//                1, TimeUnit.MINUTES
//        ).build();

        // Enqueue the work request
        WorkManager.getInstance(this).enqueue(workRequest);


    }


    private void selectImage() {
        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            // if granted
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                bitmap = BitmapFactory.decodeStream(inputStream);
                imageViewSelectedImage.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFormData() {

        Context context = getApplicationContext();
        String databasePath = context.getDatabasePath("my_database.db").getAbsolutePath();
        Toast.makeText(this, databasePath, Toast.LENGTH_SHORT).show();

        String name = editTextName.getText().toString().trim();
        String surname = editTextSurname.getText().toString().trim();

        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        RadioButton selectedGenderButton = findViewById(selectedGenderId);
        String gender = selectedGenderButton.getText().toString();


        if (name.isEmpty() || surname.isEmpty() || gender.isEmpty()) {
            Toast.makeText(this, "Please fill in all the details", Toast.LENGTH_SHORT).show();
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] image = baos.toByteArray();

        // Create or open the SQLite database
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("surname", surname);
        values.put("gender", gender);
        values.put("image", image);
        values.put("uploaded", 0);

        dbHelper.saveUser(values);

        // if online save to firestore
        if(deviceOnline()){
            Log.i(TAG,"device is already online saving...!!!");
            new OnlineStorage(context).saveToOnline();
        }else{
            initWorker();
        }
    }
// whole upload process TODO
//    private void saveToOnline() {
//
//        // Create or open the SQLite database
//        try(DatabaseHelper dbHelper = new DatabaseHelper(this.getApplicationContext());) {
//            List<ContentValues> users = dbHelper.getAllUsers3();
//
//
//            // Upload each user to Firestore
//            for (ContentValues user : users) {
//
//                if(user.getAsInteger("uploaded") == 0) {
//                    upload(user);
//                    int id = user.getAsInteger("_id");
//                    dbHelper.setUploadedStatus(id);
//                    Log.d(TAG,"Uploaded "+id);
//                }else{
//                    int id = user.getAsInteger("_id");
//                    Log.d(TAG,"Not Uploaded "+id);
//                }
//            }
//        }catch (Exception e) {
//            Log.e(TAG," error uploading "+e.getMessage());
//        }
//    }
//    private void upload(ContentValues values) {
//        // Get the FirebaseStorage instance
//        FirebaseStorage storage = FirebaseStorage.getInstance();
//
//        // Create a reference to the full path of the file, including the file name
//        StorageReference storageRef = storage.getReference().child("images/image.jpg");
//
//        Uri _selectedImageUri = selectedImageUri; //TODO
//        // Upload the image to Firebase Storage
////        UploadTask uploadTask = storageRef.putFile(_selectedImageUri);
//        UploadTask uploadTask = storageRef.putBytes(values.getAsByteArray("image"));
//
//
//
//        // Get the download URL of the uploaded image
//        Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
//            if (!task.isSuccessful()) {
//                throw Objects.requireNonNull(task.getException());
//            }
//            return storageRef.getDownloadUrl();
//        }).addOnSuccessListener(uri -> {
//            // Get the download URL of the uploaded image
//            String imageUrl = uri.toString();
//            Log.d("worktest","imageUrl "+imageUrl);
//            saveToFireStore(imageUrl, values);
//
//        }).addOnFailureListener(e -> {
//            // Handle any errors
//            Log.e("worktest",e.getMessage());
//        });
//    }
//    private void saveToFireStore(String imageUri, ContentValues values) {
//        // Create a new user with a first and last name
//        Map<String, Object> user = new HashMap<>();
//        user.put("name", values.get("name"));
//        user.put("surname", values.get("surname"));
//        user.put("gender", values.get("gender"));
//        user.put("image", imageUri);
//        user.put("uploaded", 1);
//
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        db.collection("users")
//                .add(user)
//                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
//                    @Override
//                    public void onSuccess(DocumentReference documentReference) {
//                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
////                        Toast.makeText(, "Added "+documentReference.getId(), Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.w(TAG, "Error adding document", e);
//                        Toast.makeText(getApplicationContext(), "Failed "+e.toString(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
    public boolean deviceOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
            // Device is online
            return true;
        } else {
            // Device is offline
            return false;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            // Clear the form fields
            editTextName.setText("");
            editTextSurname.setText("");
            radioGroupGender.clearCheck();
            imageViewSelectedImage.setImageResource(R.drawable.ic_launcher_foreground);
            selectedImageUri = null;
        }
    }
}
