import os
from langchain_community.document_loaders import PyPDFLoader, TextLoader, DirectoryLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_huggingface import HuggingFaceEmbeddings

class RagEngine:
    def __init__(self, data_dir="./data"):
        self.data_dir = data_dir
        self.vector_store = None
        self.embeddings = HuggingFaceEmbeddings(model_name="sentence-transformers/all-MiniLM-L6-v2")
        
        # Initialize if data exists
        if not os.path.exists(data_dir):
            os.makedirs(data_dir)
            print(f"Created data directory at {data_dir}")
        else:
            self.ingest_data()

    def ingest_data(self):
        """Loads documents from data_dir and creates a vector store."""
        print("Scanning for documents...")
        
        # 1. Load Documents (PDFs and Text files)
        pdf_loader = DirectoryLoader(self.data_dir, glob="**/*.pdf", loader_cls=PyPDFLoader)
        txt_loader = DirectoryLoader(self.data_dir, glob="**/*.txt", loader_cls=TextLoader)
        
        documents = []
        try:
            documents.extend(pdf_loader.load())
            documents.extend(txt_loader.load())
        except Exception as e:
            print(f"Error loading documents: {e}")
            return

        if not documents:
            print("No documents found in data directory.")
            return

        print(f"Loaded {len(documents)} documents.")

        # 2. Split Text
        text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
        chunks = text_splitter.split_documents(documents)
        print(f"Created {len(chunks)} text chunks.")

        # 3. Create Vector Store (FAISS)
        # Using HuggingFace embeddings (runs locally on CPU)
        print("Creating vector store (this might take a moment)...")
        self.vector_store = FAISS.from_documents(chunks, self.embeddings)
        print("Vector store created successfully.")

    def get_context(self, query, top_k=3):
        """Retrieves relevant context for a query."""
        if not self.vector_store:
            return ""
        
        try:
            # Search for similar chunks
            docs = self.vector_store.similarity_search(query, k=top_k)
            # Combine content
            context = "\n\n".join([doc.page_content for doc in docs])
            return context
        except Exception as e:
            print(f"Error retrieving context: {e}")
            return ""

    def reload(self):
        """Manually re-ingest data (useful if new files are added)."""
        self.ingest_data()
