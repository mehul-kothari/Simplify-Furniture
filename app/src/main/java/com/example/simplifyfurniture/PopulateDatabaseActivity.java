package com.example.simplifyfurniture;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class PopulateDatabaseActivity extends AppCompatActivity {
    private EditText furniture_name, furniture_descritpion;
    private ImageView furniture_photo;
    Furniture furniture;
    private Spinner furniture_type;
    private DatabaseReference mDatabase;
    private Button submit;
    private static int RESULT_LOAD_IMAGE = 1;
    Bitmap bitmap;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            furniture_photo = (ImageView) findViewById(R.id.furniture_photo);
            furniture_photo.setImageBitmap(BitmapFactory.decodeFile(picturePath));

        }
    }
        @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_populate_database);
        furniture_name = findViewById(R.id.furniture_name);
        furniture_descritpion = findViewById(R.id.furniture_description);
        furniture_photo = findViewById(R.id.furniture_photo);
        furniture_type = findViewById(R.id.furniture_type);
        submit = findViewById(R.id.submit_furniture);

        furniture_photo.setOnClickListener(v -> {

            Intent i = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, RESULT_LOAD_IMAGE);
        });
        submit.setOnClickListener(v -> {
            furniture = new Furniture();
            furniture.description = furniture_descritpion.getText().toString();
            furniture.name = furniture_name.getText().toString();
            furniture.type = FurnitureType.valueOf(furniture_type.getSelectedItem().toString().toUpperCase());
            mDatabase = FirebaseDatabase.getInstance().getReference(furniture.type.toString());
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();

            String mGroupId = mDatabase.push().getKey();
            if(furniture_photo != null) {
                Bitmap bitmap = ((BitmapDrawable) furniture_photo.getDrawable()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();
                StorageReference storageReferenceChild = storageRef.child(furniture.type + "/" + mGroupId);

                storageReferenceChild.putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        storageReferenceChild.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
//                                System.out.println(uri.toString());
                                furniture.url = uri.toString();
                                mDatabase.child(mGroupId).setValue(furniture);

                            }
                        });
                    }
                });
            }
        });

    }
}
