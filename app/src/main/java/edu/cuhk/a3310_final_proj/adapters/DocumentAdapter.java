package edu.cuhk.a3310_final_proj.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.models.Document;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder> {

    private List<Document> documents = new ArrayList<>();
    private Context context;
    private DocumentAdapterListener listener;

    public interface DocumentAdapterListener {

        void onViewDocument(Document document, int position);

        void onDeleteDocument(Document document, int position);
    }

    public DocumentAdapter(Context context, DocumentAdapterListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentViewHolder holder, int position) {
        Document document = documents.get(position);
        holder.bind(document, position);
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    public void setDocuments(List<Document> documents) {
        this.documents.clear();
        if (documents != null) {
            this.documents.addAll(documents);
        }
        notifyDataSetChanged();
    }

    public void addDocument(Document document) {
        this.documents.add(document);
        notifyItemInserted(documents.size() - 1);
    }

    public void removeDocument(int position) {
        this.documents.remove(position);
        notifyItemRemoved(position);
    }

    public List<Document> getDocuments() {
        return new ArrayList<>(documents);
    }

    class DocumentViewHolder extends RecyclerView.ViewHolder {

        TextView tvDocumentName, tvDocumentType;
        ImageButton btnView, btnDelete;

        public DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDocumentName = itemView.findViewById(R.id.tv_document_name);
            tvDocumentType = itemView.findViewById(R.id.tv_document_type);
            btnView = itemView.findViewById(R.id.btn_view_document);
            btnDelete = itemView.findViewById(R.id.btn_delete_document);
        }

        public void bind(Document document, int position) {
            tvDocumentName.setText(document.getName());

            String fileType = "Document";
            if (document.getFileUrl() != null) {
                String url = document.getFileUrl().toLowerCase();
                if (url.endsWith(".pdf")) {
                    fileType = "PDF";
                } else if (url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png")) {
                    fileType = "Image";
                } else if (url.endsWith(".docx") || url.endsWith(".doc")) {
                    fileType = "Word";
                }
            }

            tvDocumentType.setText(fileType);

            btnView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewDocument(document, position);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteDocument(document, position);
                }
            });
        }
    }
}
