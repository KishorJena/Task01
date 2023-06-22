package com.jena.task01;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OnlineStorage {

    private String TAG = "LogTask01";
    private Context _context;
    public OnlineStorage(Context context){
        _context = context;
    }

    public void saveToOnline() {

        // Create or open the SQLite database
        try(DatabaseHelper dbHelper = new DatabaseHelper(_context.getApplicationContext());) {
            List<ContentValues> users = dbHelper.getAllUsers3();


            // Upload each user to Firestore
            for (ContentValues user : users) {

                if(user.getAsInteger("uploaded") == 0) {
                    upload(user);
                    int id = user.getAsInteger("_id");
                    dbHelper.setUploadedStatus(id);
                    Log.d(TAG,"Uploaded "+id);
                }else{
                    int id = user.getAsInteger("_id");
                    Log.d(TAG,"Not Uploaded "+id);
                }
            }
        }catch (Exception e) {
            Log.e(TAG," error uploading "+e.getMessage());
        }
    }
    private void upload(ContentValues values) {
        // Get the FirebaseStorage instance
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // Create a reference to the full path of the file, including the file name
        StorageReference storageRef = storage.getReference().child("images/image.jpg");

//        Uri _selectedImageUri = selectedImageUri; //TODO
        // Upload the image to Firebase Storage
//        UploadTask uploadTask = storageRef.putFile(_selectedImageUri);
        UploadTask uploadTask = storageRef.putBytes(values.getAsByteArray("image"));



        // Get the download URL of the uploaded image
        Task<Uri> urlTask = uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            return storageRef.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            // Get the download URL of the uploaded image
            String imageUrl = uri.toString();
            Log.d("worktest","imageUrl "+imageUrl);
            saveToFireStore(imageUrl, values);

        }).addOnFailureListener(e -> {
            // Handle any errors
            Log.e("worktest",e.getMessage());
        });
    }
    private void saveToFireStore(String imageUri, ContentValues values) {
        // Create a new user with a first and last name
        Map<String, Object> user = new HashMap<>();
        user.put("name", values.get("name"));
        user.put("surname", values.get("surname"));
        user.put("gender", values.get("gender"));
        user.put("image", imageUri);
        user.put("uploaded", 1);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
//                        Toast.makeText(, "Added "+documentReference.getId(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                        Toast.makeText(_context.getApplicationContext(), "Failed "+e.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
