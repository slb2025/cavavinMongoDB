package org.example.cavavin.dal;

import com.mongodb.DuplicateKeyException;
import org.example.cavavin.bo.Bouteille;
import org.example.cavavin.bo.Couleur;
import org.example.cavavin.bo.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest()
public class BouteilleRepositoryTest {
    @Autowired
    private BouteilleRepository bouteilleRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Test
    @DisplayName("test d'ajout d'une bouteille dont le nom est déjà présent en base")
    void testAjoutBouteilleCasNomBouteilleDejaExistante(){
        //Arrange
        bouteilleRepository.deleteAll();
        Bouteille bouteille = new Bouteille("Laurent Perrier",
                2020);
        bouteilleRepository.save(bouteille);
        Bouteille bouteille2 = new Bouteille("Laurent Perrier", 2020);

        //Act
        try{
            bouteilleRepository.save(bouteille2);
            fail();
        }catch(DuplicateKeyException e){

        }

        //Assert
    }

    @Test
    void testAjoutBouteilleCasOk(){
        //Arrange
        regionRepository.deleteAll();
        Region bordeaux = regionRepository.save(new Region("Bordeaux"));
        bouteilleRepository.deleteAll();
        Bouteille bouteille = new Bouteille("Château Cheval Blanc",
                2015);
        bouteille.setRegion(bordeaux);
        bouteille.setCouleur(new Couleur("Rouge"));
        //Act
        Bouteille newBouteille = bouteilleRepository.save(bouteille);
        //Assert
        Optional<Bouteille> optBouteille = bouteilleRepository.findById(newBouteille.getId());
        assertTrue(optBouteille.isPresent());
        assertNotNull(optBouteille.get().getRegion());
        assertEquals(newBouteille.getRegion(), optBouteille.get().getRegion());

    }


}