// Dans org.example.cavavin.exception
package org.example.cavavin.service.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, String id) {
        super(String.format("%s non trouvée avec l'ID : %s", resourceName, id));
    }
}