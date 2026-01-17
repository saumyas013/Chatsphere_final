//package com.collegeproject.chatgptclone.repository;
//
//import com.collegeproject.chatgptclone.model.ChatMessage;
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
///**
// * Spring Data MongoDB repository for ChatMessage documents.
// * Provides standard CRUD operations and custom query methods.
// */
//@Repository // Marks this interface as a Spring Data repository
//public interface ChatRepository extends MongoRepository<ChatMessage, String> {
//
//    /**
//     * Finds all chat messages, ordered by timestamp in ascending order.
//     * This is useful for retrieving the chat history in chronological order.
//     *
//     * @return A list of ChatMessage objects, sorted by timestamp.
//     */
//    List<ChatMessage> findAllByOrderByTimestampAsc();
//}

package com.collegeproject.chatgptclone.repository;

import com.collegeproject.chatgptclone.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for ChatMessage documents.
 * Provides standard CRUD operations and custom query methods.
 */
@Repository // Marks this interface as a Spring Data repository
public interface ChatRepository extends MongoRepository<ChatMessage, String> {

    /**
     * Finds all chat messages, ordered by timestamp in ascending order.
     * This is useful for retrieving the chat history in chronological order.
     *
     * @return A list of ChatMessage objects, sorted by timestamp.
     */
    // Keep this if you need to retrieve all messages (e.g., for admin)
    List<ChatMessage> findAllByOrderByTimestampAsc();

    /**
     * NEW: Finds all chat messages for a specific user, ordered by timestamp.
     *
     * @param userId The ID of the user whose messages are to be retrieved.
     * @return A list of ChatMessage objects for the given user, sorted by
     *         timestamp.
     */
    List<ChatMessage> findByUserIdOrderByTimestampAsc(String userId);

    /**
     * Finds the last 10 chat messages for a specific user, ordered by timestamp
     * descending.
     * Used for building conversational context.
     */
    List<ChatMessage> findTop10ByUserIdOrderByTimestampDesc(String userId);

    /**
     * Finds the last 5 chat messages for a specific user, ordered by timestamp
     * descending.
     * Used for optimized conversational context.
     */
    List<ChatMessage> findTop5ByUserIdOrderByTimestampDesc(String userId);
}
