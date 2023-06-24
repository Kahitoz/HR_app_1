package Shared.Backend11_Utils.Hire;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import Shared.Backend11_Utils.Applicant.Backend11_Applicant_cAdapter;
import Shared.Backend11_Utils.Applicant.Backend11_Applicant_getSet;
import Shared.Get_Date;

public class Backend11_Hire_Handler {
    Context context;
    FirebaseAuth auth;
    FirebaseFirestore database;
    ImageView b11_back, b11_send;
    ProgressBar b11_progress;
    EditText b11_message;
    RecyclerView b11_recycle;
    String r_doc_id,r_user_id;

    public Backend11_Hire_Handler(Context context, FirebaseAuth auth, FirebaseFirestore database,
                                  ImageView b11_back, ImageView b11_send, ProgressBar b11_progress,
                                  EditText b11_message, RecyclerView b11_recycle,
                                  String r_doc_id, String r_user_id) {
        this.context = context;
        this.auth = auth;
        this.database = database;
        this.b11_back = b11_back;
        this.b11_send = b11_send;
        this.b11_progress = b11_progress;
        this.b11_message = b11_message;
        this.b11_recycle = b11_recycle;
        this.r_doc_id = r_doc_id;
        this.r_user_id = r_user_id;
    }

    public void send_chat() {
        Toast.makeText(context,"sending", Toast.LENGTH_SHORT).show();
        Get_Date get_date = new Get_Date();
        String get_message = b11_message.getText().toString();
        String date = get_date.getCurrentDate();

        // Get the user's name asynchronously
        get_name().thenAccept(name -> {
            if (get_message.length() == 0) {
                b11_message.setError("Cannot be blank");
            } else {
                Map<String, Object> chat = new HashMap<>();
                chat.put("date", date);
                chat.put("name", name);
                chat.put("message", get_message);
                chat.put("timestamp", FieldValue.serverTimestamp()); // Add timestamp field

                database.collection("users").document(Objects.requireNonNull(auth.getUid()))
                        .collection("jobPosted").document(r_user_id.toString().trim())
                        .collection("Applied_Job").document(r_doc_id.toString().trim())
                        .collection("chat").add(chat) // Use add() instead of document()
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show();
                            b11_message.setText("");
                        });
            }
        }).exceptionally(ex -> {
            // Handle any errors retrieving the name
            ex.printStackTrace();
            return null;
        });
    }


    public CompletableFuture<String> get_name() {
        CompletableFuture<String> future = new CompletableFuture<>();

        database.collection("users")
                .document(Objects.requireNonNull(auth.getUid()))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot != null) {
                            String name = snapshot.getString("name");
                            future.complete(name); // Complete the CompletableFuture with the retrieved name
                        } else {
                            future.completeExceptionally(new Exception("User document not found"));
                        }
                    } else {
                        future.completeExceptionally(task.getException());
                    }
                });

        return future;
    }

    public void fetch_chat() {
        database.collection("users").document(Objects.requireNonNull(auth.getUid()))
                .collection("jobPosted").document(r_user_id.toString().trim())
                .collection("Applied_Job").document(r_doc_id.toString().trim())
                .collection("chat")
                .orderBy("timestamp") // Order the documents by the "timestamp" field in ascending order
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Backend11_Applicant_getSet> documentList = new ArrayList<>();

                            for (DocumentSnapshot document : task.getResult()) {
                                Backend11_Applicant_getSet backend5Document = document.toObject(Backend11_Applicant_getSet.class);
                                documentList.add(backend5Document);
                            }

                            Backend11_Applicant_cAdapter adapter = new Backend11_Applicant_cAdapter(documentList);
                            b11_recycle.setAdapter(adapter);
                            b11_recycle.setLayoutManager(new LinearLayoutManager(b11_recycle.getContext()));
                        } else {
                            Toast.makeText(context, "No documents present", Toast.LENGTH_SHORT).show();
                        }
                        b11_progress.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                        b11_progress.setVisibility(View.GONE);
                    }
                });
    }


}
