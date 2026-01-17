//package com.collegeproject.chatgptclone.repository;
//
//import com.collegeproject.chatgptclone.model.QnADocument;
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//
///**
// * Spring Data MongoDB repository for QnADocument documents.
// * Provides standard CRUD operations and custom query methods for Q&A.
// */
//@Repository // Marks this interface as a Spring Data repository
//public interface QnARepository extends MongoRepository<QnADocument, String> {
//
//    /**
//     * Finds a QnADocument by its question, ignoring case.
//     * This allows for flexible matching of user input to predefined questions.
//     *
//     * @param question The question string to search for.
//     * @return An Optional containing the QnADocument if found, otherwise empty.
//     */
//    Optional<QnADocument> findByQuestionIgnoreCase(String question);
//}
