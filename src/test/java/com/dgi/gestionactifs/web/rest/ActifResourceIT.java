package com.dgi.gestionactifs.web.rest;

import static com.dgi.gestionactifs.domain.ActifAsserts.*;
import static com.dgi.gestionactifs.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dgi.gestionactifs.IntegrationTest;
import com.dgi.gestionactifs.domain.Actif;
import com.dgi.gestionactifs.domain.enumeration.StatutActif;
import com.dgi.gestionactifs.domain.enumeration.TypeActif;
import com.dgi.gestionactifs.repository.ActifRepository;
import com.dgi.gestionactifs.security.AuthoritiesConstants;
import com.dgi.gestionactifs.service.dto.ActifDTO;
import com.dgi.gestionactifs.service.mapper.ActifMapper;
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
 * Integration tests for the {@link ActifResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class ActifResourceIT {

    private static final String DEFAULT_IDENTIFIANT_UNIQUE = "AAAAAAAAAA";
    private static final String UPDATED_IDENTIFIANT_UNIQUE = "BBBBBBBBBB";

    private static final String DEFAULT_CODE_BARRE_QR = "AAAAAAAAAA";
    private static final String UPDATED_CODE_BARRE_QR = "BBBBBBBBBB";

    private static final TypeActif DEFAULT_TYPE = TypeActif.POSTE_TRAVAIL;
    private static final TypeActif UPDATED_TYPE = TypeActif.IMPRIMANTE;

    private static final StatutActif DEFAULT_ETAT = StatutActif.EN_SERVICE;
    private static final StatutActif UPDATED_ETAT = StatutActif.EN_MAINTENANCE;

    private static final String DEFAULT_LOCALISATION = "AAAAAAAAAA";
    private static final String UPDATED_LOCALISATION = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_DATE_ACQUISITION = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE_ACQUISITION = LocalDate.parse("2026-09-02");
    private static final LocalDate SMALLER_DATE_ACQUISITION = LocalDate.ofEpochDay(-1L);

    private static final String ENTITY_API_URL = "/api/actifs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ActifRepository actifRepository;

    @Autowired
    private ActifMapper actifMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restActifMockMvc;

    private Actif actif;

    private Actif insertedActif;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Actif createEntity() {
        return new Actif()
            .identifiantUnique(DEFAULT_IDENTIFIANT_UNIQUE)
            .codeBarreQR(DEFAULT_CODE_BARRE_QR)
            .type(DEFAULT_TYPE)
            .etat(DEFAULT_ETAT)
            .localisation(DEFAULT_LOCALISATION)
            .dateAcquisition(DEFAULT_DATE_ACQUISITION);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Actif createUpdatedEntity() {
        return new Actif()
            .identifiantUnique(UPDATED_IDENTIFIANT_UNIQUE)
            .codeBarreQR(UPDATED_CODE_BARRE_QR)
            .type(UPDATED_TYPE)
            .etat(UPDATED_ETAT)
            .localisation(UPDATED_LOCALISATION)
            .dateAcquisition(UPDATED_DATE_ACQUISITION);
    }

    @BeforeEach
    void initTest() {
        actif = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedActif != null) {
            actifRepository.delete(insertedActif);
            insertedActif = null;
        }
    }

    @Test
    @Transactional
    void createActif() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Actif
        ActifDTO actifDTO = actifMapper.toDto(actif);
        var returnedActifDTO = om.readValue(
            restActifMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actifDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ActifDTO.class
        );

        // Validate the Actif in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedActif = actifMapper.toEntity(returnedActifDTO);
        assertActifUpdatableFieldsEquals(returnedActif, getPersistedActif(returnedActif));

        insertedActif = returnedActif;
    }

    @Test
    @Transactional
    void createActifWithExistingId() throws Exception {
        // Create the Actif with an existing ID
        actif.setId(1L);
        ActifDTO actifDTO = actifMapper.toDto(actif);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restActifMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actifDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Actif in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkIdentifiantUniqueIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        actif.setIdentifiantUnique(null);

        // Create the Actif, which fails.
        ActifDTO actifDTO = actifMapper.toDto(actif);

        restActifMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actifDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkTypeIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        actif.setType(null);

        // Create the Actif, which fails.
        ActifDTO actifDTO = actifMapper.toDto(actif);

        restActifMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actifDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkEtatIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        actif.setEtat(null);

        // Create the Actif, which fails.
        ActifDTO actifDTO = actifMapper.toDto(actif);

        restActifMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actifDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllActifs() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList
        restActifMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(actif.getId().intValue())))
            .andExpect(jsonPath("$.[*].identifiantUnique").value(hasItem(DEFAULT_IDENTIFIANT_UNIQUE)))
            .andExpect(jsonPath("$.[*].codeBarreQR").value(hasItem(DEFAULT_CODE_BARRE_QR)))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())))
            .andExpect(jsonPath("$.[*].localisation").value(hasItem(DEFAULT_LOCALISATION)))
            .andExpect(jsonPath("$.[*].dateAcquisition").value(hasItem(DEFAULT_DATE_ACQUISITION.toString())));
    }

    @Test
    @Transactional
    void getActif() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get the actif
        restActifMockMvc
            .perform(get(ENTITY_API_URL_ID, actif.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(actif.getId().intValue()))
            .andExpect(jsonPath("$.identifiantUnique").value(DEFAULT_IDENTIFIANT_UNIQUE))
            .andExpect(jsonPath("$.codeBarreQR").value(DEFAULT_CODE_BARRE_QR))
            .andExpect(jsonPath("$.type").value(DEFAULT_TYPE.toString()))
            .andExpect(jsonPath("$.etat").value(DEFAULT_ETAT.toString()))
            .andExpect(jsonPath("$.localisation").value(DEFAULT_LOCALISATION))
            .andExpect(jsonPath("$.dateAcquisition").value(DEFAULT_DATE_ACQUISITION.toString()));
    }

    @Test
    @Transactional
    void getActifsByIdFiltering() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        Long id = actif.getId();

        defaultActifFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultActifFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultActifFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllActifsByIdentifiantUniqueIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where identifiantUnique equals to
        defaultActifFiltering(
            "identifiantUnique.equals=" + DEFAULT_IDENTIFIANT_UNIQUE,
            "identifiantUnique.equals=" + UPDATED_IDENTIFIANT_UNIQUE
        );
    }

    @Test
    @Transactional
    void getAllActifsByIdentifiantUniqueIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where identifiantUnique in
        defaultActifFiltering(
            "identifiantUnique.in=" + DEFAULT_IDENTIFIANT_UNIQUE + "," + UPDATED_IDENTIFIANT_UNIQUE,
            "identifiantUnique.in=" + UPDATED_IDENTIFIANT_UNIQUE
        );
    }

    @Test
    @Transactional
    void getAllActifsByIdentifiantUniqueIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where identifiantUnique is not null
        defaultActifFiltering("identifiantUnique.specified=true", "identifiantUnique.specified=false");
    }

    @Test
    @Transactional
    void getAllActifsByIdentifiantUniqueContainsSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where identifiantUnique contains
        defaultActifFiltering(
            "identifiantUnique.contains=" + DEFAULT_IDENTIFIANT_UNIQUE,
            "identifiantUnique.contains=" + UPDATED_IDENTIFIANT_UNIQUE
        );
    }

    @Test
    @Transactional
    void getAllActifsByIdentifiantUniqueNotContainsSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where identifiantUnique does not contain
        defaultActifFiltering(
            "identifiantUnique.doesNotContain=" + UPDATED_IDENTIFIANT_UNIQUE,
            "identifiantUnique.doesNotContain=" + DEFAULT_IDENTIFIANT_UNIQUE
        );
    }

    @Test
    @Transactional
    void getAllActifsByCodeBarreQRIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where codeBarreQR equals to
        defaultActifFiltering("codeBarreQR.equals=" + DEFAULT_CODE_BARRE_QR, "codeBarreQR.equals=" + UPDATED_CODE_BARRE_QR);
    }

    @Test
    @Transactional
    void getAllActifsByCodeBarreQRIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where codeBarreQR in
        defaultActifFiltering(
            "codeBarreQR.in=" + DEFAULT_CODE_BARRE_QR + "," + UPDATED_CODE_BARRE_QR,
            "codeBarreQR.in=" + UPDATED_CODE_BARRE_QR
        );
    }

    @Test
    @Transactional
    void getAllActifsByCodeBarreQRIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where codeBarreQR is not null
        defaultActifFiltering("codeBarreQR.specified=true", "codeBarreQR.specified=false");
    }

    @Test
    @Transactional
    void getAllActifsByCodeBarreQRContainsSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where codeBarreQR contains
        defaultActifFiltering("codeBarreQR.contains=" + DEFAULT_CODE_BARRE_QR, "codeBarreQR.contains=" + UPDATED_CODE_BARRE_QR);
    }

    @Test
    @Transactional
    void getAllActifsByCodeBarreQRNotContainsSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where codeBarreQR does not contain
        defaultActifFiltering("codeBarreQR.doesNotContain=" + UPDATED_CODE_BARRE_QR, "codeBarreQR.doesNotContain=" + DEFAULT_CODE_BARRE_QR);
    }

    @Test
    @Transactional
    void getAllActifsByTypeIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where type equals to
        defaultActifFiltering("type.equals=" + DEFAULT_TYPE, "type.equals=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllActifsByTypeIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where type in
        defaultActifFiltering("type.in=" + DEFAULT_TYPE + "," + UPDATED_TYPE, "type.in=" + UPDATED_TYPE);
    }

    @Test
    @Transactional
    void getAllActifsByTypeIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where type is not null
        defaultActifFiltering("type.specified=true", "type.specified=false");
    }

    @Test
    @Transactional
    void getAllActifsByEtatIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where etat equals to
        defaultActifFiltering("etat.equals=" + DEFAULT_ETAT, "etat.equals=" + UPDATED_ETAT);
    }

    @Test
    @Transactional
    void getAllActifsByEtatIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where etat in
        defaultActifFiltering("etat.in=" + DEFAULT_ETAT + "," + UPDATED_ETAT, "etat.in=" + UPDATED_ETAT);
    }

    @Test
    @Transactional
    void getAllActifsByEtatIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where etat is not null
        defaultActifFiltering("etat.specified=true", "etat.specified=false");
    }

    @Test
    @Transactional
    void getAllActifsByLocalisationIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where localisation equals to
        defaultActifFiltering("localisation.equals=" + DEFAULT_LOCALISATION, "localisation.equals=" + UPDATED_LOCALISATION);
    }

    @Test
    @Transactional
    void getAllActifsByLocalisationIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where localisation in
        defaultActifFiltering(
            "localisation.in=" + DEFAULT_LOCALISATION + "," + UPDATED_LOCALISATION,
            "localisation.in=" + UPDATED_LOCALISATION
        );
    }

    @Test
    @Transactional
    void getAllActifsByLocalisationIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where localisation is not null
        defaultActifFiltering("localisation.specified=true", "localisation.specified=false");
    }

    @Test
    @Transactional
    void getAllActifsByLocalisationContainsSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where localisation contains
        defaultActifFiltering("localisation.contains=" + DEFAULT_LOCALISATION, "localisation.contains=" + UPDATED_LOCALISATION);
    }

    @Test
    @Transactional
    void getAllActifsByLocalisationNotContainsSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where localisation does not contain
        defaultActifFiltering("localisation.doesNotContain=" + UPDATED_LOCALISATION, "localisation.doesNotContain=" + DEFAULT_LOCALISATION);
    }

    @Test
    @Transactional
    void getAllActifsByDateAcquisitionIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where dateAcquisition equals to
        defaultActifFiltering("dateAcquisition.equals=" + DEFAULT_DATE_ACQUISITION, "dateAcquisition.equals=" + UPDATED_DATE_ACQUISITION);
    }

    @Test
    @Transactional
    void getAllActifsByDateAcquisitionIsInShouldWork() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where dateAcquisition in
        defaultActifFiltering(
            "dateAcquisition.in=" + DEFAULT_DATE_ACQUISITION + "," + UPDATED_DATE_ACQUISITION,
            "dateAcquisition.in=" + UPDATED_DATE_ACQUISITION
        );
    }

    @Test
    @Transactional
    void getAllActifsByDateAcquisitionIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where dateAcquisition is not null
        defaultActifFiltering("dateAcquisition.specified=true", "dateAcquisition.specified=false");
    }

    @Test
    @Transactional
    void getAllActifsByDateAcquisitionIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where dateAcquisition is greater than or equal to
        defaultActifFiltering(
            "dateAcquisition.greaterThanOrEqual=" + DEFAULT_DATE_ACQUISITION,
            "dateAcquisition.greaterThanOrEqual=" + UPDATED_DATE_ACQUISITION
        );
    }

    @Test
    @Transactional
    void getAllActifsByDateAcquisitionIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where dateAcquisition is less than or equal to
        defaultActifFiltering(
            "dateAcquisition.lessThanOrEqual=" + DEFAULT_DATE_ACQUISITION,
            "dateAcquisition.lessThanOrEqual=" + SMALLER_DATE_ACQUISITION
        );
    }

    @Test
    @Transactional
    void getAllActifsByDateAcquisitionIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where dateAcquisition is less than
        defaultActifFiltering(
            "dateAcquisition.lessThan=" + UPDATED_DATE_ACQUISITION,
            "dateAcquisition.lessThan=" + DEFAULT_DATE_ACQUISITION
        );
    }

    @Test
    @Transactional
    void getAllActifsByDateAcquisitionIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        // Get all the actifList where dateAcquisition is greater than
        defaultActifFiltering(
            "dateAcquisition.greaterThan=" + SMALLER_DATE_ACQUISITION,
            "dateAcquisition.greaterThan=" + DEFAULT_DATE_ACQUISITION
        );
    }

    private void defaultActifFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultActifShouldBeFound(shouldBeFound);
        defaultActifShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultActifShouldBeFound(String filter) throws Exception {
        restActifMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(actif.getId().intValue())))
            .andExpect(jsonPath("$.[*].identifiantUnique").value(hasItem(DEFAULT_IDENTIFIANT_UNIQUE)))
            .andExpect(jsonPath("$.[*].codeBarreQR").value(hasItem(DEFAULT_CODE_BARRE_QR)))
            .andExpect(jsonPath("$.[*].type").value(hasItem(DEFAULT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].etat").value(hasItem(DEFAULT_ETAT.toString())))
            .andExpect(jsonPath("$.[*].localisation").value(hasItem(DEFAULT_LOCALISATION)))
            .andExpect(jsonPath("$.[*].dateAcquisition").value(hasItem(DEFAULT_DATE_ACQUISITION.toString())));

        // Check, that the count call also returns 1
        restActifMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultActifShouldNotBeFound(String filter) throws Exception {
        restActifMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restActifMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingActif() throws Exception {
        // Get the actif
        restActifMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingActif() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the actif
        Actif updatedActif = actifRepository.findById(actif.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedActif are not directly saved in db
        em.detach(updatedActif);
        updatedActif
            .identifiantUnique(UPDATED_IDENTIFIANT_UNIQUE)
            .codeBarreQR(UPDATED_CODE_BARRE_QR)
            .type(UPDATED_TYPE)
            .etat(UPDATED_ETAT)
            .localisation(UPDATED_LOCALISATION)
            .dateAcquisition(UPDATED_DATE_ACQUISITION);
        ActifDTO actifDTO = actifMapper.toDto(updatedActif);

        restActifMockMvc
            .perform(
                put(ENTITY_API_URL_ID, actifDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actifDTO))
            )
            .andExpect(status().isOk());

        // Validate the Actif in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedActifToMatchAllProperties(updatedActif);
    }

    @Test
    @Transactional
    void putNonExistingActif() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actif.setId(longCount.incrementAndGet());

        // Create the Actif
        ActifDTO actifDTO = actifMapper.toDto(actif);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restActifMockMvc
            .perform(
                put(ENTITY_API_URL_ID, actifDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actifDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Actif in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchActif() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actif.setId(longCount.incrementAndGet());

        // Create the Actif
        ActifDTO actifDTO = actifMapper.toDto(actif);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restActifMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(actifDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Actif in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamActif() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actif.setId(longCount.incrementAndGet());

        // Create the Actif
        ActifDTO actifDTO = actifMapper.toDto(actif);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restActifMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(actifDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Actif in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateActifWithPatch() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the actif using partial update
        Actif partialUpdatedActif = new Actif();
        partialUpdatedActif.setId(actif.getId());

        partialUpdatedActif
            .identifiantUnique(UPDATED_IDENTIFIANT_UNIQUE)
            .localisation(UPDATED_LOCALISATION)
            .dateAcquisition(UPDATED_DATE_ACQUISITION);

        restActifMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedActif.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedActif))
            )
            .andExpect(status().isOk());

        // Validate the Actif in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertActifUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedActif, actif), getPersistedActif(actif));
    }

    @Test
    @Transactional
    void fullUpdateActifWithPatch() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the actif using partial update
        Actif partialUpdatedActif = new Actif();
        partialUpdatedActif.setId(actif.getId());

        partialUpdatedActif
            .identifiantUnique(UPDATED_IDENTIFIANT_UNIQUE)
            .codeBarreQR(UPDATED_CODE_BARRE_QR)
            .type(UPDATED_TYPE)
            .etat(UPDATED_ETAT)
            .localisation(UPDATED_LOCALISATION)
            .dateAcquisition(UPDATED_DATE_ACQUISITION);

        restActifMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedActif.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedActif))
            )
            .andExpect(status().isOk());

        // Validate the Actif in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertActifUpdatableFieldsEquals(partialUpdatedActif, getPersistedActif(partialUpdatedActif));
    }

    @Test
    @Transactional
    void patchNonExistingActif() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actif.setId(longCount.incrementAndGet());

        // Create the Actif
        ActifDTO actifDTO = actifMapper.toDto(actif);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restActifMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, actifDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(actifDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Actif in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchActif() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actif.setId(longCount.incrementAndGet());

        // Create the Actif
        ActifDTO actifDTO = actifMapper.toDto(actif);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restActifMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(actifDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Actif in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamActif() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        actif.setId(longCount.incrementAndGet());

        // Create the Actif
        ActifDTO actifDTO = actifMapper.toDto(actif);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restActifMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(actifDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Actif in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteActif() throws Exception {
        // Initialize the database
        insertedActif = actifRepository.saveAndFlush(actif);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the actif
        restActifMockMvc
            .perform(delete(ENTITY_API_URL_ID, actif.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return actifRepository.count();
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

    protected Actif getPersistedActif(Actif actif) {
        return actifRepository.findById(actif.getId()).orElseThrow();
    }

    protected void assertPersistedActifToMatchAllProperties(Actif expectedActif) {
        assertActifAllPropertiesEquals(expectedActif, getPersistedActif(expectedActif));
    }

    protected void assertPersistedActifToMatchUpdatableProperties(Actif expectedActif) {
        assertActifAllUpdatablePropertiesEquals(expectedActif, getPersistedActif(expectedActif));
    }
}
