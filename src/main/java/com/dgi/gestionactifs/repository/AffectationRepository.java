package com.dgi.gestionactifs.repository;

import com.dgi.gestionactifs.domain.Affectation;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Affectation entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AffectationRepository extends JpaRepository<Affectation, Long>, JpaSpecificationExecutor<Affectation> {
    @Query("select affectation from Affectation affectation where affectation.utilisateur.login = ?#{authentication.name}")
    List<Affectation> findByUtilisateurIsCurrentUser();

    /**
     * Trouve les affectations actives (sans date de restitution) d'un utilisateur donne.
     * Utilise pour le filtrage 'mes actifs' cote Agent.
     */
    java.util.List<com.dgi.gestionactifs.domain.Affectation> findByUtilisateur_LoginAndDateRestitutionIsNull(String login);

    /**
     * Trouve toutes les affectations d'un utilisateur donne (pour le filtrage 'les miennes' cote Agent).
     */
    java.util.List<com.dgi.gestionactifs.domain.Affectation> findByUtilisateur_Login(String login);
}
