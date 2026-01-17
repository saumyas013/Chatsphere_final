from flask import Flask, request, jsonify
import ollama
import time
from rag_utils import RagEngine

app = Flask(__name__)

# Initialize RAG Engine
# It will load PDFs/Text from ./data folder
print("Initializing RAG Engine...")
rag_engine = RagEngine()

@app.route('/predict', methods=['POST'])
def generate_response():
    try:
        # 1. Get the message sent from Spring Boot
        data = request.json
        user_message = data.get('message', '')
        image_data = data.get('image') # Extract image data (base64)
        history = data.get('history', []) # Extract history list

        print(f"Received message: {user_message}")
        if history:
            print(f"Received history of {len(history)} messages.")

        # 2. Construct Prompt with History and Context
        
        # Start with System Prompt
        full_prompt = (
            "You are a helpful assistant. \n"
            "Use the provided CHAT HISTORY and DOCUMENT CONTEXT to answer the user.\n"
            "1. Prioritize CHAT HISTORY for personal details.\n"
            "2. Use DOCUMENT CONTEXT for factual queries.\n"
            "3. If irrelevant, ignore the context.\n"
            "4. IMPORTANT: If the user provides a Multiple Choice Question (MCQ), you MUST output the Correct Option(s), the Answer text, and a short explanation.\n\n"
        )

        # Append History
        if history:
            full_prompt += "--- CHAT HISTORY ---\n"
            for msg in history:
                role = "User" if msg.get('role') == 'user' else "Assistant"
                content = msg.get('content', '')
                full_prompt += f"{role}: {content}\n"
            full_prompt += "--------------------\n\n"

        # Handle RAG (Always retrieve context now, as per user request)
        context = ""
        # We always try to get context, but maybe use a generic query if just image?
        # Actually, user message is usually present with image.
        if user_message:
            start_rag = time.time()
            context = rag_engine.get_context(user_message)
            print(f"⏱️ RAG Context Retrieval took: {time.time() - start_rag:.2f} seconds")
        
        if context:
            full_prompt += f"--- DOCUMENT CONTEXT ---\n{context}\n------------------------\n\n"

        # Handle Current Message
        current_instruction = user_message
        
        if image_data:
             # Check for OCR command
            if user_message.strip() == "[[OCR_MODE]]":
                current_instruction = "Transcribe the text in this image verbatim."
            else:
                base_instruction = "First, carefully read the text in the image. Then, answer the question found in the text. if it is a multiple-choice question, provide the correct option and answer."
                if not user_message.strip():
                    current_instruction = base_instruction
                else:
                    # forceful instruction for MCQs
                    current_instruction = (
                        f"User Question: {user_message}\n\n"
                        "IMPORTANT SYSTEM INSTRUCTION:\n"
                        "1. Analyze the image carefully.\n"
                        "2. This is a MULTI-SELECT question. Evaluate EACH option individually.\n"
                        "3. Step-by-step reasoning required:\n"
                        "   - Option A: [Correct/Incorrect] because...\n"
                        "   - Option B: [Correct/Incorrect] because...\n"
                        "   - ... (repeat for all options)\n"
                        "4. Final Conclusion:\n"
                        "   - Correct Option(s): [List ALL verified options, e.g., A & C]"
                    )
        
        # Detect if user wants to CREATE/GENERATE a quiz
        is_quiz_creation = any(keyword in user_message.lower() for keyword in ["create quiz", "generate quiz", "make a quiz", "create a test", "create mcq"])
        
        if is_quiz_creation:
            current_instruction = (
                f"User Request: {user_message}\n\n"
                "IMPORTANT SYSTEM INSTRUCTION:\n"
                "The user wants you to GENERATE a quiz based on the 'DOCUMENT CONTEXT' provided above.\n"
                "1. Create the requested number of Multiple Choice Questions.\n"
                "2. Use the content from the documents.\n"
                "3. Format each question clearly with Options (A, B, C, D).\n"
                "4. Provide the correct answer and a brief explanation at the end of the quiz."
            )
        
        # Detect if text message looks like an MCQ (only if NOT creating one)
        elif not image_data and any(marker in user_message for marker in ["A)", "B)", "C)", "1.", "2.", "3.","a.","b.","c.","option"]):
             current_instruction = (
                f"User Question: {user_message}\n\n"
                "IMPORTANT SYSTEM INSTRUCTION:\n"
                "This looks like a Multiple Choice Question.\n"
                "1. Search 'DOCUMENT CONTEXT' for the answer.\n"
                "2. EVALUATE EACH OPTION SEPARATELY.\n"
                "   - Check if Option 1 is supported by context.\n"
                "   - Check if Option 2 is supported by context.\n"
                "   - ... and so on.\n"
                "3. Final Answer:\n"
                "   - Provide Correct Option(s) and Explanation."
            )

        full_prompt += f"User: {current_instruction}\nAssistant:"

        print("--- FINAL PROMPT SENT TO LLM ---")
        print(full_prompt)
        print("--------------------------------")

        # 3. Build Payload
        msg_payload = {
            'role': 'user',
            'content': full_prompt
        }
        
        if image_data:
            msg_payload['images'] = [image_data]

        messages_payload = [msg_payload]

        # 4. Send to Ollama
        print("⏳ Sending request to Ollama (this may take time)...")
        start_llm = time.time()
        response = ollama.chat(model='llava', messages=messages_payload)
        print(f"⏱️ Ollama Inference took: {time.time() - start_llm:.2f} seconds")

        # 3. Extract the text reply from Ollama
        bot_reply = response['message']['content']
        print(f"LLaVA Reply: {bot_reply}")

        # 4. Return it back to Java
        return jsonify({"response": bot_reply})

    except Exception as e:
        print(f"Error: {e}")
        return jsonify({"response": "Error processing your request with LLaVA."}), 500

@app.route('/reload-docs', methods=['POST'])
def reload_docs():
    """Endpoint to trigger re-indexing of documents."""
    try:
        rag_engine.reload()
        return jsonify({"status": "Documents reloaded successfully."})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    # Run this Flask server on port 5000
    app.run(host='0.0.0.0', port=5000)