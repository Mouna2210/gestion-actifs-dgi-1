package com.dgi.gestionactifs.web.rest;

import static com.dgi.gestionactifs.domain.AffectationAsserts.*;
import static com.dgi.gestionactifs.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dgi.gestionactifs.IntegrationTest;
import com.dgi.gestionactifs.domain.Actif;
import com.dgi.gestionactifs.domain.Affectation;
import com.dgi.gestionactifs.domain.User;
import com.dgi.gestionactifs.repository.AffectationRepository;
import com.dgi.gestionactifs.repository.UserRepository;
import com.dgi.gestionactifs.security.AuthoritiesConstants;
import com.dgi.gestionactifs.service.dto.AffectationDTO;
import com.dgi.gestionactifs.service.mapper.AffectationMapper;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link AffectationResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class AffectationResourceIT {

    private static final LocalDate DEFAULT_DATE_AFFECTATION = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE_AFFECTATION = LocalDate.parse("2026-09-02");
    private static final LocalDate SMALLER_DATE_AFFECTATION = LocalDate.ofEpochDay(-1L);

    private static final LocalDate DEFAULT_DATE_RESTITUTION = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE_RESTITUTION = LocalDate.parse("2026-09-02");
    private static final LocalDate SMALLER_DATE_RESTITUTION = LocalDate.ofEpochDay(-1L);

    private static final String DEFAULT_NUMERO_BORDEREAU = "AAAAAAAAAA";
    private static final String UPDATED_NUMERO_BORDEREAU = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/affectations";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private AffectationRepository affectationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AffectationMapper affectationMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restAffectationMockMvc;

    private Affectation affectation;

    private Affectation insertedAffectation;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Affectation createEntity(EntityManager em) {
        Affectation affectation = new Affectation()
            .dateAffectation(DEFAULT_DATE_AFFECTATION)
            .dateRestitution(DEFAULT_DATE_RESTITUTION)
            .numeroBordereau(DEFAULT_NUMERO_BORDEREAU);
        // Add required entity
        Actif actif;
        if (TestUtil.findAll(em, Actif.class).isEmpty()) {
            actif = ActifResourceIT.createEntity();
            em.persist(actif);
            em.flush();
        } else {
            actif = TestUtil.findAll(em, Actif.class).get(0);
        }
        affectation.setActif(actif);
        return affectation;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Affectation createUpdatedEntity(EntityManager em) {
        Affectation updatedAffectation = new Affectation()
            .dateAffectation(UPDATED_DATE_AFFECTATION)
            .dateRestitution(UPDATED_DATE_RESTITUTION)
            .numeroBordereau(UPDATED_NUMERO_BORDEREAU);
        // Add required entity
        Actif actif;
        if (TestUtil.findAll(em, Actif.class).isEmpty()) {
            actif = ActifResourceIT.createUpdatedEntity();
            em.persist(actif);
            em.flush();
        } else {
            actif = TestUtil.findAll(em, Actif.class).get(0);
        }
        updatedAffectation.setActif(actif);
        return updatedAffectation;
    }

    @BeforeEach
    void initTest() {
        affectation = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedAffectation != null) {
            affectationRepository.delete(insertedAffectation);
            insertedAffectation = null;
        }
    }

    @Test
    @Transactional
    void createAffectation() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Affectation
        AffectationDTO affectationDTO = affectationMapper.toDto(affectation);
        var returnedAffectationDTO = om.readValue(
            restAffectationMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(affectationDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            AffectationDTO.class
        );

        // Validate the Affectation in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedAffectation = affectationMapper.toEntity(returnedAffectationDTO);
        assertAffectationUpdatableFieldsEquals(returnedAffectation, getPersistedAffectation(returnedAffectation));

        insertedAffectation = returnedAffectation;
    }

    @Test
    @Transactional
    void createAffectationWithExistingId() throws Exception {
        // Create the Affectation with an existing ID
        affectation.setId(1L);
        AffectationDTO affectationDTO = affectationMapper.toDto(affectation);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restAffectationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(affectationDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Affectation in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkDateAffectationIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        affectation.setDateAffectation(null);

        // Create the Affectation, which fails.
        AffectationDTO affectationDTO = affectationMapper.toDto(affectation);

        restAffectationMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(affectationDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllAffectations() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList
        restAffectationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(affectation.getId().intValue())))
            .andExpect(jsonPath("$.[*].dateAffectation").value(hasItem(DEFAULT_DATE_AFFECTATION.toString())))
            .andExpect(jsonPath("$.[*].dateRestitution").value(hasItem(DEFAULT_DATE_RESTITUTION.toString())))
            .andExpect(jsonPath("$.[*].numeroBordereau").value(hasItem(DEFAULT_NUMERO_BORDEREAU)));
    }

    @Test
    @Transactional
    void getAffectation() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get the affectation
        restAffectationMockMvc
            .perform(get(ENTITY_API_URL_ID, affectation.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(affectation.getId().intValue()))
            .andExpect(jsonPath("$.dateAffectation").value(DEFAULT_DATE_AFFECTATION.toString()))
            .andExpect(jsonPath("$.dateRestitution").value(DEFAULT_DATE_RESTITUTION.toString()))
            .andExpect(jsonPath("$.numeroBordereau").value(DEFAULT_NUMERO_BORDEREAU));
    }

    @Test
    @Transactional
    void getAffectationsByIdFiltering() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        Long id = affectation.getId();

        defaultAffectationFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultAffectationFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultAffectationFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllAffectationsByDateAffectationIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateAffectation equals to
        defaultAffectationFiltering(
            "dateAffectation.equals=" + DEFAULT_DATE_AFFECTATION,
            "dateAffectation.equals=" + UPDATED_DATE_AFFECTATION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByDateAffectationIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateAffectation in
        defaultAffectationFiltering(
            "dateAffectation.in=" + DEFAULT_DATE_AFFECTATION + "," + UPDATED_DATE_AFFECTATION,
            "dateAffectation.in=" + UPDATED_DATE_AFFECTATION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByDateAffectationIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateAffectation is not null
        defaultAffectationFiltering("dateAffectation.specified=true", "dateAffectation.specified=false");
    }

    @Test
    @Transactional
    void getAllAffectationsByDateAffectationIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateAffectation is greater than or equal to
        defaultAffectationFiltering(
            "dateAffectation.greaterThanOrEqual=" + DEFAULT_DATE_AFFECTATION,
            "dateAffectation.greaterThanOrEqual=" + UPDATED_DATE_AFFECTATION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByDateAffectationIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateAffectation is less than or equal to
        defaultAffectationFiltering(
            "dateAffectation.lessThanOrEqual=" + DEFAULT_DATE_AFFECTATION,
            "dateAffectation.lessThanOrEqual=" + SMALLER_DATE_AFFECTATION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByDateAffectationIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateAffectation is less than
        defaultAffectationFiltering(
            "dateAffectation.lessThan=" + UPDATED_DATE_AFFECTATION,
            "dateAffectation.lessThan=" + DEFAULT_DATE_AFFECTATION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByDateAffectationIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateAffectation is greater than
        defaultAffectationFiltering(
            "dateAffectation.greaterThan=" + SMALLER_DATE_AFFECTATION,
            "dateAffectation.greaterThan=" + DEFAULT_DATE_AFFECTATION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByDateRestitutionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateRestitution equals to
        defaultAffectationFiltering(
            "dateRestitution.equals=" + DEFAULT_DATE_RESTITUTION,
            "dateRestitution.equals=" + UPDATED_DATE_RESTITUTION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByDateRestitutionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateRestitution in
        defaultAffectationFiltering(
            "dateRestitution.in=" + DEFAULT_DATE_RESTITUTION + "," + UPDATED_DATE_RESTITUTION,
            "dateRestitution.in=" + UPDATED_DATE_RESTITUTION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByDateRestitutionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateRestitution is not null
        defaultAffectationFiltering("dateRestitution.specified=true", "dateRestitution.specified=false");
    }

    @Test
    @Transactional
    void getAllAffectationsByDateRestitutionIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateRestitution is greater than or equal to
        defaultAffectationFiltering(
            "dateRestitution.greaterThanOrEqual=" + DEFAULT_DATE_RESTITUTION,
            "dateRestitution.greaterThanOrEqual=" + UPDATED_DATE_RESTITUTION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByDateRestitutionIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateRestitution is less than or equal to
        defaultAffectationFiltering(
            "dateRestitution.lessThanOrEqual=" + DEFAULT_DATE_RESTITUTION,
            "dateRestitution.lessThanOrEqual=" + SMALLER_DATE_RESTITUTION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByDateRestitutionIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateRestitution is less than
        defaultAffectationFiltering(
            "dateRestitution.lessThan=" + UPDATED_DATE_RESTITUTION,
            "dateRestitution.lessThan=" + DEFAULT_DATE_RESTITUTION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByDateRestitutionIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where dateRestitution is greater than
        defaultAffectationFiltering(
            "dateRestitution.greaterThan=" + SMALLER_DATE_RESTITUTION,
            "dateRestitution.greaterThan=" + DEFAULT_DATE_RESTITUTION
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByNumeroBordereauIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where numeroBordereau equals to
        defaultAffectationFiltering(
            "numeroBordereau.equals=" + DEFAULT_NUMERO_BORDEREAU,
            "numeroBordereau.equals=" + UPDATED_NUMERO_BORDEREAU
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByNumeroBordereauIsInShouldWork() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where numeroBordereau in
        defaultAffectationFiltering(
            "numeroBordereau.in=" + DEFAULT_NUMERO_BORDEREAU + "," + UPDATED_NUMERO_BORDEREAU,
            "numeroBordereau.in=" + UPDATED_NUMERO_BORDEREAU
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByNumeroBordereauIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where numeroBordereau is not null
        defaultAffectationFiltering("numeroBordereau.specified=true", "numeroBordereau.specified=false");
    }

    @Test
    @Transactional
    void getAllAffectationsByNumeroBordereauContainsSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where numeroBordereau contains
        defaultAffectationFiltering(
            "numeroBordereau.contains=" + DEFAULT_NUMERO_BORDEREAU,
            "numeroBordereau.contains=" + UPDATED_NUMERO_BORDEREAU
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByNumeroBordereauNotContainsSomething() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        // Get all the affectationList where numeroBordereau does not contain
        defaultAffectationFiltering(
            "numeroBordereau.doesNotContain=" + UPDATED_NUMERO_BORDEREAU,
            "numeroBordereau.doesNotContain=" + DEFAULT_NUMERO_BORDEREAU
        );
    }

    @Test
    @Transactional
    void getAllAffectationsByUtilisateurIsEqualToSomething() throws Exception {
        User utilisateur;
        if (TestUtil.findAll(em, User.class).isEmpty()) {
            affectationRepository.saveAndFlush(affectation);
            utilisateur = UserResourceIT.createEntity();
        } else {
            utilisateur = TestUtil.findAll(em, User.class).get(0);
        }
        em.persist(utilisateur);
        em.flush();
        affectation.setUtilisateur(utilisateur);
        affectationRepository.saveAndFlush(affectation);
        Long utilisateurId = utilisateur.getId();
        // Get all the affectationList where utilisateur equals to utilisateurId
        defaultAffectationShouldBeFound("utilisateurId.equals=" + utilisateurId);

        // Get all the affectationList where utilisateur equals to (utilisateurId + 1)
        defaultAffectationShouldNotBeFound("utilisateurId.equals=" + (utilisateurId + 1));
    }

    @Test
    @Transactional
    void getAllAffectationsByActifIsEqualToSomething() throws Exception {
        Actif actif;
        if (TestUtil.findAll(em, Actif.class).isEmpty()) {
            affectationRepository.saveAndFlush(affectation);
            actif = ActifResourceIT.createEntity();
        } else {
            actif = TestUtil.findAll(em, Actif.class).get(0);
        }
        em.persist(actif);
        em.flush();
        affectation.setActif(actif);
        affectationRepository.saveAndFlush(affectation);
        Long actifId = actif.getId();
        // Get all the affectationList where actif equals to actifId
        defaultAffectationShouldBeFound("actifId.equals=" + actifId);

        // Get all the affectationList where actif equals to (actifId + 1)
        defaultAffectationShouldNotBeFound("actifId.equals=" + (actifId + 1));
    }

    private void defaultAffectationFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultAffectationShouldBeFound(shouldBeFound);
        defaultAffectationShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultAffectationShouldBeFound(String filter) throws Exception {
        restAffectationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(affectation.getId().intValue())))
            .andExpect(jsonPath("$.[*].dateAffectation").value(hasItem(DEFAULT_DATE_AFFECTATION.toString())))
            .andExpect(jsonPath("$.[*].dateRestitution").value(hasItem(DEFAULT_DATE_RESTITUTION.toString())))
            .andExpect(jsonPath("$.[*].numeroBordereau").value(hasItem(DEFAULT_NUMERO_BORDEREAU)));

        // Check, that the count call also returns 1
        restAffectationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultAffectationShouldNotBeFound(String filter) throws Exception {
        restAffectationMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restAffectationMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingAffectation() throws Exception {
        // Get the affectation
        restAffectationMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingAffectation() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the affectation
        Affectation updatedAffectation = affectationRepository.findById(affectation.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedAffectation are not directly saved in db
        em.detach(updatedAffectation);
        updatedAffectation
            .dateAffectation(UPDATED_DATE_AFFECTATION)
            .dateRestitution(UPDATED_DATE_RESTITUTION)
            .numeroBordereau(UPDATED_NUMERO_BORDEREAU);
        AffectationDTO affectationDTO = affectationMapper.toDto(updatedAffectation);

        restAffectationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, affectationDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(affectationDTO))
            )
            .andExpect(status().isOk());

        // Validate the Affectation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedAffectationToMatchAllProperties(updatedAffectation);
    }

    @Test
    @Transactional
    void putNonExistingAffectation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        affectation.setId(longCount.incrementAndGet());

        // Create the Affectation
        AffectationDTO affectationDTO = affectationMapper.toDto(affectation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAffectationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, affectationDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(affectationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Affectation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchAffectation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        affectation.setId(longCount.incrementAndGet());

        // Create the Affectation
        AffectationDTO affectationDTO = affectationMapper.toDto(affectation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAffectationMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(affectationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Affectation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamAffectation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        affectation.setId(longCount.incrementAndGet());

        // Create the Affectation
        AffectationDTO affectationDTO = affectationMapper.toDto(affectation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAffectationMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(affectationDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Affectation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateAffectationWithPatch() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the affectation using partial update
        Affectation partialUpdatedAffectation = new Affectation();
        partialUpdatedAffectation.setId(affectation.getId());

        restAffectationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAffectation.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAffectation))
            )
            .andExpect(status().isOk());

        // Validate the Affectation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAffectationUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedAffectation, affectation),
            getPersistedAffectation(affectation)
        );
    }

    @Test
    @Transactional
    void fullUpdateAffectationWithPatch() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the affectation using partial update
        Affectation partialUpdatedAffectation = new Affectation();
        partialUpdatedAffectation.setId(affectation.getId());

        partialUpdatedAffectation
            .dateAffectation(UPDATED_DATE_AFFECTATION)
            .dateRestitution(UPDATED_DATE_RESTITUTION)
            .numeroBordereau(UPDATED_NUMERO_BORDEREAU);

        restAffectationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedAffectation.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedAffectation))
            )
            .andExpect(status().isOk());

        // Validate the Affectation in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertAffectationUpdatableFieldsEquals(partialUpdatedAffectation, getPersistedAffectation(partialUpdatedAffectation));
    }

    @Test
    @Transactional
    void patchNonExistingAffectation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        affectation.setId(longCount.incrementAndGet());

        // Create the Affectation
        AffectationDTO affectationDTO = affectationMapper.toDto(affectation);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restAffectationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, affectationDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(affectationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Affectation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchAffectation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        affectation.setId(longCount.incrementAndGet());

        // Create the Affectation
        AffectationDTO affectationDTO = affectationMapper.toDto(affectation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAffectationMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(affectationDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Affectation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamAffectation() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        affectation.setId(longCount.incrementAndGet());

        // Create the Affectation
        AffectationDTO affectationDTO = affectationMapper.toDto(affectation);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restAffectationMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(affectationDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Affectation in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteAffectation() throws Exception {
        // Initialize the database
        insertedAffectation = affectationRepository.saveAndFlush(affectation);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the affectation
        restAffectationMockMvc
            .perform(delete(ENTITY_API_URL_ID, affectation.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return affectationRepository.count();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Affectation getPersistedAffectation(Affectation affectation) {
        return affectationRepository.findById(affectation.getId()).orElseThrow();
    }

    protected void assertPersistedAffectationToMatchAllProperties(Affectation expectedAffectation) {
        assertAffectationAllPropertiesEquals(expectedAffectation, getPersistedAffectation(expectedAffectation));
    }

    protected void assertPersistedAffectationToMatchUpdatableProperties(Affectation expectedAffectation) {
        assertAffectationAllUpdatablePropertiesEquals(expectedAffectation, getPersistedAffectation(expectedAffectation));
    }
}
