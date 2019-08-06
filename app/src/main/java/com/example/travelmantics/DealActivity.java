package com.example.travelmantics;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class DealActivity extends AppCompatActivity {
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private static final int PICTURE_RESULT = 42;
    EditText textTitle;
    EditText textDescription;
    EditText textPrice;
    ImageView imageView;
    TravelDeal deal;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        //FirebaseUtil.openFbReference("traveldeals");
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        textTitle = (EditText) findViewById(R.id.txtTitle);
        textDescription = (EditText) findViewById(R.id.txtDescription);
        textPrice = (EditText) findViewById(R.id.txtPrice);
        imageView = (ImageView) findViewById(R.id.image);
        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if (deal == null){
            deal = new TravelDeal();
        }
        this.deal = deal;
        textTitle.setText(deal.getTitle());
        textDescription.setText(deal.getDescription());
        textPrice.setText("$"+ deal.getPrice().toString());
        showImage(deal.getImageUrl());
        Button btnImage = findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
                startActivityForResult(intent.createChooser(intent,
                        "Insert Picture"),PICTURE_RESULT);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this,"Deal Saved",Toast.LENGTH_LONG).show();
                clean();
                backToList();
                return true;

            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this,"Deal Deleted",Toast.LENGTH_LONG).show();
                backToList();
            default:
                return super.onOptionsItemSelected(item);


        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu,menu);
        if (FirebaseUtil.isAdmin){
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditTexts(true);
            findViewById(R.id.btnImage).setEnabled(true);

        }
        else{
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditTexts(false);
            findViewById(R.id.btnImage).setEnabled(false);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT && resultCode == RESULT_OK) {
            assert data != null;
            Uri imageUri = data.getData();
            assert imageUri != null;
            final StorageReference reference = FirebaseUtil.mStorageReference.
                    child(Objects.requireNonNull(imageUri.getLastPathSegment()));

            final UploadTask uploadTask = reference.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    //There is an error.
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Otherwise continue with the task to get the download URL
                    return reference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        String downloadUri = Objects.requireNonNull(task.getResult()).toString();
                        String pictureName = uploadTask.getSnapshot().getStorage().getPath();

                        deal.setImageUrl(downloadUri);
                        deal.setImageName(pictureName);
                        DealActivity.this.showImage(downloadUri);
                    } else {
                        Toast.makeText(DealActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }


    private void saveDeal() {
       deal.setTitle(textTitle.getText().toString());
       deal.setPrice(textPrice.getText().toString());
       deal.setDescription(textDescription.getText().toString());
        //String title = textTitle.getText().toString();
        //String description = textDescription.getText().toString();
        //String price = textPrice.getText().toString();
        if (deal.getId() == null){
            mDatabaseReference.push().setValue(deal);

        }
        else{
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }
    }
    private void deleteDeal() {
        if (deal == null) {
            Toast.makeText(this, "Error. Deal does not exist", Toast.LENGTH_LONG).show();
            return;
        }
        mDatabaseReference.child(deal.getId()).removeValue();

        if (deal.getImageName() != null && !deal.getImageName().isEmpty()) {
            StorageReference picRef = FirebaseUtil.mFirebaseStorage.getReference().
                    child(deal.getImageName());
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(DealActivity.this, "Delete image Successful",
                            Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(DealActivity.this, "Delete image failed",
                            Toast.LENGTH_SHORT).show();
                    //Log.e(TAG, "onFailure: -------------" + e.getMessage());
                }
            });
        }
    }
    private void backToList() {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }



    private void clean(){
        textTitle.setText("");
        textPrice.setText("");
        textDescription.setText("");
        textTitle.requestFocus();

    }
    private void enableEditTexts(boolean isEnabled){
        textTitle.setEnabled(isEnabled);
        textPrice.setEnabled(isEnabled);
        textDescription.setEnabled(isEnabled);

    }

    private void showImage(String url) {
        if (url != null && !url.isEmpty()) {
            // int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get()
                    .load(url)
                    //.resize(width, width * 2 / 3)
                    .placeholder(R.drawable.loading_animation)
                    .error(R.drawable.ic_error_outline)
                    //.centerCrop()
                    .into(imageView);
        } else {
            Toast.makeText(this, "Url is null", Toast.LENGTH_LONG).show();
        }
    }




}
