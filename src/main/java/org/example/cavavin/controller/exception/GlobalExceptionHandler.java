package org.example.cavavin.controller.exception;

import org.example.cavavin.service.exception.ResourceNotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Gestionnaire d'exceptions global pour l'API REST.
 * Il intercepte les exceptions levées par les couches Service/DAL
 * et les traduit en codes de statut HTTP appropriés.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Intercepte la ResourceNotFoundException (exception métier) et la mappe à 404 NOT FOUND.
     * Cette exception est levée par BouteilleService.findById().
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException ex) {
        // Loggez l'erreur ici si nécessaire
        System.err.println("Erreur 404 levée : " + ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Intercepte la DuplicateKeyException (exception Spring Data pour l'index unique MongoDB)
     * et la mappe à 409 CONFLICT (conflit de ressource existante).
     * Ceci gère les POST/PUT qui violent l'index unique (nom de bouteille ou de région).
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<String> handleDuplicateKeyException(DuplicateKeyException ex) {
        // Pour un message plus lisible, on peut essayer d'extraire la clé dupliquée du message MongoDB
        String errorMessage = "Erreur de conflit (Index unique) : La ressource existe déjà ou est en double.";

        // Tentative d'extraire le champ et la valeur concernés pour plus de clarté
        String fullMessage = ex.getMostSpecificCause().getMessage();
        if (fullMessage != null && fullMessage.contains("dup key")) {
            // Exemple : E11000 duplicate key error collection: cavavin.bouteilles index: nom dup key: { nom: "..." }
            errorMessage = "Conflit de données : " + fullMessage.substring(fullMessage.indexOf("index:")).replace("'", "");
        }

        System.err.println("Erreur 409 levée : " + errorMessage);
        return new ResponseEntity<>(errorMessage, HttpStatus.CONFLICT);
    }

    // Ajoutez d'autres gestionnaires ici pour les exceptions courantes (IllegalArgumentException -> 400 BAD REQUEST)
}