package com.dgi.gestionactifs.web.rest;

import static com.dgi.gestionactifs.domain.MaintenanceAsserts.*;
import static com.dgi.gestionactifs.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dgi.gestionactifs.IntegrationTest;
import com.dgi.gestionactifs.domain.Actif;
import com.dgi.gestionactifs.domain.Maintenance;
import com.dgi.gestionactifs.domain.User;
import com.dgi.gestionactifs.domain.enumeration.StatutMaintenance;
import com.dgi.gestionactifs.domain.enumeration.TypeMaintenance;
import com.dgi.gestionactifs.repository.MaintenanceRepository;
import com.dgi.gestionactifs.repository.UserRepository;
import com.dgi.gestionactifs.security.AuthoritiesConstants;
import com.dgi.gestionactifs.service.dto.MaintenanceDTO;
import com.dgi.gestionactifs.service.mapper.MaintenanceMapper;
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
 * Integration tests for the {@link MaintenanceResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class MaintenanceResourceIT {

    private static final TypeMaintenance DEFAULT_TYPE_MAINTENANCE = TypeMaintenance.PREVENTIVE;
    private static final TypeMaintenance UPDATED_TYPE_MAINTENANCE = TypeMaintenance.CORRECTIVE;

    private static final LocalDate DEFAULT_DATE_PANNE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE_PANNE = LocalDate.parse("2026-09-02");
    private static final LocalDate SMALLER_DATE_PANNE = LocalDate.ofEpochDay(-1L);

    private static final StatutMaintenance DEFAULT_STATUT = StatutMaintenance.OUVERTE;
    private static final StatutMaintenance UPDATED_STATUT = StatutMaintenance.EN_COURS;

    private static final String DEFAULT_COMPTE_RENDU = "AAAAAAAAAA";
    private static final String UPDATED_COMPTE_RENDU = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_DATE_CLOTURE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE_CLOTURE = LocalDate.parse("2026-09-02");
    private static final LocalDate SMALLER_DATE_CLOTURE = LocalDate.ofEpochDay(-1L);

    private static final String ENTITY_API_URL = "/api/maintenances";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MaintenanceMapper maintenanceMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restMaintenanceMockMvc;

    private Maintenance maintenance;

    private Maintenance insertedMaintenance;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Maintenance createEntity(EntityManager em) {
        Maintenance maintenance = new Maintenance()
            .typeMaintenance(DEFAULT_TYPE_MAINTENANCE)
            .datePanne(DEFAULT_DATE_PANNE)
            .statut(DEFAULT_STATUT)
            .compteRendu(DEFAULT_COMPTE_RENDU)
            .dateCloture(DEFAULT_DATE_CLOTURE);
        // Add required entity
        Actif actif;
        if (TestUtil.findAll(em, Actif.class).isEmpty()) {
            actif = ActifResourceIT.createEntity();
            em.persist(actif);
            em.flush();
        } else {
            actif = TestUtil.findAll(em, Actif.class).get(0);
        }
        maintenance.setActif(actif);
        return maintenance;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Maintenance createUpdatedEntity(EntityManager em) {
        Maintenance updatedMaintenance = new Maintenance()
            .typeMaintenance(UPDATED_TYPE_MAINTENANCE)
            .datePanne(UPDATED_DATE_PANNE)
            .statut(UPDATED_STATUT)
            .compteRendu(UPDATED_COMPTE_RENDU)
            .dateCloture(UPDATED_DATE_CLOTURE);
        // Add required entity
        Actif actif;
        if (TestUtil.findAll(em, Actif.class).isEmpty()) {
            actif = ActifResourceIT.createUpdatedEntity();
            em.persist(actif);
            em.flush();
        } else {
            actif = TestUtil.findAll(em, Actif.class).get(0);
        }
        updatedMaintenance.setActif(actif);
        return updatedMaintenance;
    }

    @BeforeEach
    void initTest() {
        maintenance = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedMaintenance != null) {
            maintenanceRepository.delete(insertedMaintenance);
            insertedMaintenance = null;
        }
    }

    @Test
    @Transactional
    void createMaintenance() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Maintenance
        MaintenanceDTO maintenanceDTO = maintenanceMapper.toDto(maintenance);
        var returnedMaintenanceDTO = om.readValue(
            restMaintenanceMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(maintenanceDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            MaintenanceDTO.class
        );

        // Validate the Maintenance in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedMaintenance = maintenanceMapper.toEntity(returnedMaintenanceDTO);
        assertMaintenanceUpdatableFieldsEquals(returnedMaintenance, getPersistedMaintenance(returnedMaintenance));

        insertedMaintenance = returnedMaintenance;
    }

    @Test
    @Transactional
    void createMaintenanceWithExistingId() throws Exception {
        // Create the Maintenance with an existing ID
        maintenance.setId(1L);
        MaintenanceDTO maintenanceDTO = maintenanceMapper.toDto(maintenance);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restMaintenanceMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(maintenanceDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Maintenance in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTypeMaintenanceIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        maintenance.setTypeMaintenance(null);

        // Create the Maintenance, which fails.
        MaintenanceDTO maintenanceDTO = maintenanceMapper.toDto(maintenance);

        restMaintenanceMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(maintenanceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkStatutIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        maintenance.setStatut(null);

        // Create the Maintenance, which fails.
        MaintenanceDTO maintenanceDTO = maintenanceMapper.toDto(maintenance);

        restMaintenanceMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(maintenanceDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllMaintenances() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList
        restMaintenanceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(maintenance.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeMaintenance").value(hasItem(DEFAULT_TYPE_MAINTENANCE.toString())))
            .andExpect(jsonPath("$.[*].datePanne").value(hasItem(DEFAULT_DATE_PANNE.toString())))
            .andExpect(jsonPath("$.[*].statut").value(hasItem(DEFAULT_STATUT.toString())))
            .andExpect(jsonPath("$.[*].compteRendu").value(hasItem(DEFAULT_COMPTE_RENDU)))
            .andExpect(jsonPath("$.[*].dateCloture").value(hasItem(DEFAULT_DATE_CLOTURE.toString())));
    }

    @Test
    @Transactional
    @WithMockUser(username = "agent", authorities = { "ROLE_AGENT" })
    void getAllMaintenancesForAgentWithoutAssignedActifsReturnsEmptyList() throws Exception {
        restMaintenanceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Transactional
    void getMaintenance() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get the maintenance
        restMaintenanceMockMvc
            .perform(get(ENTITY_API_URL_ID, maintenance.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(maintenance.getId().intValue()))
            .andExpect(jsonPath("$.typeMaintenance").value(DEFAULT_TYPE_MAINTENANCE.toString()))
            .andExpect(jsonPath("$.datePanne").value(DEFAULT_DATE_PANNE.toString()))
            .andExpect(jsonPath("$.statut").value(DEFAULT_STATUT.toString()))
            .andExpect(jsonPath("$.compteRendu").value(DEFAULT_COMPTE_RENDU))
            .andExpect(jsonPath("$.dateCloture").value(DEFAULT_DATE_CLOTURE.toString()));
    }

    @Test
    @Transactional
    void getMaintenancesByIdFiltering() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        Long id = maintenance.getId();

        defaultMaintenanceFiltering("id.equals=" + id, "id.notEquals=" + id);

        defaultMaintenanceFiltering("id.greaterThanOrEqual=" + id, "id.greaterThan=" + id);

        defaultMaintenanceFiltering("id.lessThanOrEqual=" + id, "id.lessThan=" + id);
    }

    @Test
    @Transactional
    void getAllMaintenancesByTypeMaintenanceIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where typeMaintenance equals to
        defaultMaintenanceFiltering(
            "typeMaintenance.equals=" + DEFAULT_TYPE_MAINTENANCE,
            "typeMaintenance.equals=" + UPDATED_TYPE_MAINTENANCE
        );
    }

    @Test
    @Transactional
    void getAllMaintenancesByTypeMaintenanceIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where typeMaintenance in
        defaultMaintenanceFiltering(
            "typeMaintenance.in=" + DEFAULT_TYPE_MAINTENANCE + "," + UPDATED_TYPE_MAINTENANCE,
            "typeMaintenance.in=" + UPDATED_TYPE_MAINTENANCE
        );
    }

    @Test
    @Transactional
    void getAllMaintenancesByTypeMaintenanceIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where typeMaintenance is not null
        defaultMaintenanceFiltering("typeMaintenance.specified=true", "typeMaintenance.specified=false");
    }

    @Test
    @Transactional
    void getAllMaintenancesByDatePanneIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where datePanne equals to
        defaultMaintenanceFiltering("datePanne.equals=" + DEFAULT_DATE_PANNE, "datePanne.equals=" + UPDATED_DATE_PANNE);
    }

    @Test
    @Transactional
    void getAllMaintenancesByDatePanneIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where datePanne in
        defaultMaintenanceFiltering("datePanne.in=" + DEFAULT_DATE_PANNE + "," + UPDATED_DATE_PANNE, "datePanne.in=" + UPDATED_DATE_PANNE);
    }

    @Test
    @Transactional
    void getAllMaintenancesByDatePanneIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where datePanne is not null
        defaultMaintenanceFiltering("datePanne.specified=true", "datePanne.specified=false");
    }

    @Test
    @Transactional
    void getAllMaintenancesByDatePanneIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where datePanne is greater than or equal to
        defaultMaintenanceFiltering(
            "datePanne.greaterThanOrEqual=" + DEFAULT_DATE_PANNE,
            "datePanne.greaterThanOrEqual=" + UPDATED_DATE_PANNE
        );
    }

    @Test
    @Transactional
    void getAllMaintenancesByDatePanneIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where datePanne is less than or equal to
        defaultMaintenanceFiltering("datePanne.lessThanOrEqual=" + DEFAULT_DATE_PANNE, "datePanne.lessThanOrEqual=" + SMALLER_DATE_PANNE);
    }

    @Test
    @Transactional
    void getAllMaintenancesByDatePanneIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where datePanne is less than
        defaultMaintenanceFiltering("datePanne.lessThan=" + UPDATED_DATE_PANNE, "datePanne.lessThan=" + DEFAULT_DATE_PANNE);
    }

    @Test
    @Transactional
    void getAllMaintenancesByDatePanneIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where datePanne is greater than
        defaultMaintenanceFiltering("datePanne.greaterThan=" + SMALLER_DATE_PANNE, "datePanne.greaterThan=" + DEFAULT_DATE_PANNE);
    }

    @Test
    @Transactional
    void getAllMaintenancesByStatutIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where statut equals to
        defaultMaintenanceFiltering("statut.equals=" + DEFAULT_STATUT, "statut.equals=" + UPDATED_STATUT);
    }

    @Test
    @Transactional
    void getAllMaintenancesByStatutIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where statut in
        defaultMaintenanceFiltering("statut.in=" + DEFAULT_STATUT + "," + UPDATED_STATUT, "statut.in=" + UPDATED_STATUT);
    }

    @Test
    @Transactional
    void getAllMaintenancesByStatutIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where statut is not null
        defaultMaintenanceFiltering("statut.specified=true", "statut.specified=false");
    }

    @Test
    @Transactional
    void getAllMaintenancesByDateClotureIsEqualToSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where dateCloture equals to
        defaultMaintenanceFiltering("dateCloture.equals=" + DEFAULT_DATE_CLOTURE, "dateCloture.equals=" + UPDATED_DATE_CLOTURE);
    }

    @Test
    @Transactional
    void getAllMaintenancesByDateClotureIsInShouldWork() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where dateCloture in
        defaultMaintenanceFiltering(
            "dateCloture.in=" + DEFAULT_DATE_CLOTURE + "," + UPDATED_DATE_CLOTURE,
            "dateCloture.in=" + UPDATED_DATE_CLOTURE
        );
    }

    @Test
    @Transactional
    void getAllMaintenancesByDateClotureIsNullOrNotNull() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where dateCloture is not null
        defaultMaintenanceFiltering("dateCloture.specified=true", "dateCloture.specified=false");
    }

    @Test
    @Transactional
    void getAllMaintenancesByDateClotureIsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where dateCloture is greater than or equal to
        defaultMaintenanceFiltering(
            "dateCloture.greaterThanOrEqual=" + DEFAULT_DATE_CLOTURE,
            "dateCloture.greaterThanOrEqual=" + UPDATED_DATE_CLOTURE
        );
    }

    @Test
    @Transactional
    void getAllMaintenancesByDateClotureIsLessThanOrEqualToSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where dateCloture is less than or equal to
        defaultMaintenanceFiltering(
            "dateCloture.lessThanOrEqual=" + DEFAULT_DATE_CLOTURE,
            "dateCloture.lessThanOrEqual=" + SMALLER_DATE_CLOTURE
        );
    }

    @Test
    @Transactional
    void getAllMaintenancesByDateClotureIsLessThanSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where dateCloture is less than
        defaultMaintenanceFiltering("dateCloture.lessThan=" + UPDATED_DATE_CLOTURE, "dateCloture.lessThan=" + DEFAULT_DATE_CLOTURE);
    }

    @Test
    @Transactional
    void getAllMaintenancesByDateClotureIsGreaterThanSomething() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        // Get all the maintenanceList where dateCloture is greater than
        defaultMaintenanceFiltering("dateCloture.greaterThan=" + SMALLER_DATE_CLOTURE, "dateCloture.greaterThan=" + DEFAULT_DATE_CLOTURE);
    }

    @Test
    @Transactional
    void getAllMaintenancesByTechnicienIsEqualToSomething() throws Exception {
        User technicien;
        if (TestUtil.findAll(em, User.class).isEmpty()) {
            maintenanceRepository.saveAndFlush(maintenance);
            technicien = UserResourceIT.createEntity();
        } else {
            technicien = TestUtil.findAll(em, User.class).get(0);
        }
        em.persist(technicien);
        em.flush();
        maintenance.setTechnicien(technicien);
        maintenanceRepository.saveAndFlush(maintenance);
        Long technicienId = technicien.getId();
        // Get all the maintenanceList where technicien equals to technicienId
        defaultMaintenanceShouldBeFound("technicienId.equals=" + technicienId);

        // Get all the maintenanceList where technicien equals to (technicienId + 1)
        defaultMaintenanceShouldNotBeFound("technicienId.equals=" + (technicienId + 1));
    }

    @Test
    @Transactional
    void getAllMaintenancesByActifIsEqualToSomething() throws Exception {
        Actif actif;
        if (TestUtil.findAll(em, Actif.class).isEmpty()) {
            maintenanceRepository.saveAndFlush(maintenance);
            actif = ActifResourceIT.createEntity();
        } else {
            actif = TestUtil.findAll(em, Actif.class).get(0);
        }
        em.persist(actif);
        em.flush();
        maintenance.setActif(actif);
        maintenanceRepository.saveAndFlush(maintenance);
        Long actifId = actif.getId();
        // Get all the maintenanceList where actif equals to actifId
        defaultMaintenanceShouldBeFound("actifId.equals=" + actifId);

        // Get all the maintenanceList where actif equals to (actifId + 1)
        defaultMaintenanceShouldNotBeFound("actifId.equals=" + (actifId + 1));
    }

    private void defaultMaintenanceFiltering(String shouldBeFound, String shouldNotBeFound) throws Exception {
        defaultMaintenanceShouldBeFound(shouldBeFound);
        defaultMaintenanceShouldNotBeFound(shouldNotBeFound);
    }

    /**
     * Executes the search, and checks that the default entity is returned.
     */
    private void defaultMaintenanceShouldBeFound(String filter) throws Exception {
        restMaintenanceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(maintenance.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeMaintenance").value(hasItem(DEFAULT_TYPE_MAINTENANCE.toString())))
            .andExpect(jsonPath("$.[*].datePanne").value(hasItem(DEFAULT_DATE_PANNE.toString())))
            .andExpect(jsonPath("$.[*].statut").value(hasItem(DEFAULT_STATUT.toString())))
            .andExpect(jsonPath("$.[*].compteRendu").value(hasItem(DEFAULT_COMPTE_RENDU)))
            .andExpect(jsonPath("$.[*].dateCloture").value(hasItem(DEFAULT_DATE_CLOTURE.toString())));

        // Check, that the count call also returns 1
        restMaintenanceMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("1"));
    }

    /**
     * Executes the search, and checks that the default entity is not returned.
     */
    private void defaultMaintenanceShouldNotBeFound(String filter) throws Exception {
        restMaintenanceMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        // Check, that the count call also returns 0
        restMaintenanceMockMvc
            .perform(get(ENTITY_API_URL + "/count?sort=id,desc&" + filter))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().string("0"));
    }

    @Test
    @Transactional
    void getNonExistingMaintenance() throws Exception {
        // Get the maintenance
        restMaintenanceMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingMaintenance() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the maintenance
        Maintenance updatedMaintenance = maintenanceRepository.findById(maintenance.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedMaintenance are not directly saved in db
        em.detach(updatedMaintenance);
        updatedMaintenance
            .typeMaintenance(UPDATED_TYPE_MAINTENANCE)
            .datePanne(UPDATED_DATE_PANNE)
            .statut(UPDATED_STATUT)
            .compteRendu(UPDATED_COMPTE_RENDU)
            .dateCloture(UPDATED_DATE_CLOTURE);
        MaintenanceDTO maintenanceDTO = maintenanceMapper.toDto(updatedMaintenance);

        restMaintenanceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, maintenanceDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(maintenanceDTO))
            )
            .andExpect(status().isOk());

        // Validate the Maintenance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedMaintenanceToMatchAllProperties(updatedMaintenance);
    }

    @Test
    @Transactional
    void putNonExistingMaintenance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        maintenance.setId(longCount.incrementAndGet());

        // Create the Maintenance
        MaintenanceDTO maintenanceDTO = maintenanceMapper.toDto(maintenance);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMaintenanceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, maintenanceDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(maintenanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Maintenance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchMaintenance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        maintenance.setId(longCount.incrementAndGet());

        // Create the Maintenance
        MaintenanceDTO maintenanceDTO = maintenanceMapper.toDto(maintenance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMaintenanceMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(maintenanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Maintenance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamMaintenance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        maintenance.setId(longCount.incrementAndGet());

        // Create the Maintenance
        MaintenanceDTO maintenanceDTO = maintenanceMapper.toDto(maintenance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMaintenanceMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(maintenanceDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Maintenance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateMaintenanceWithPatch() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the maintenance using partial update
        Maintenance partialUpdatedMaintenance = new Maintenance();
        partialUpdatedMaintenance.setId(maintenance.getId());

        partialUpdatedMaintenance.typeMaintenance(UPDATED_TYPE_MAINTENANCE).statut(UPDATED_STATUT);

        restMaintenanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMaintenance.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMaintenance))
            )
            .andExpect(status().isOk());

        // Validate the Maintenance in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMaintenanceUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedMaintenance, maintenance),
            getPersistedMaintenance(maintenance)
        );
    }

    @Test
    @Transactional
    void fullUpdateMaintenanceWithPatch() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the maintenance using partial update
        Maintenance partialUpdatedMaintenance = new Maintenance();
        partialUpdatedMaintenance.setId(maintenance.getId());

        partialUpdatedMaintenance
            .typeMaintenance(UPDATED_TYPE_MAINTENANCE)
            .datePanne(UPDATED_DATE_PANNE)
            .statut(UPDATED_STATUT)
            .compteRendu(UPDATED_COMPTE_RENDU)
            .dateCloture(UPDATED_DATE_CLOTURE);

        restMaintenanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedMaintenance.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedMaintenance))
            )
            .andExpect(status().isOk());

        // Validate the Maintenance in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMaintenanceUpdatableFieldsEquals(partialUpdatedMaintenance, getPersistedMaintenance(partialUpdatedMaintenance));
    }

    @Test
    @Transactional
    void patchNonExistingMaintenance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        maintenance.setId(longCount.incrementAndGet());

        // Create the Maintenance
        MaintenanceDTO maintenanceDTO = maintenanceMapper.toDto(maintenance);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMaintenanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, maintenanceDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(maintenanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Maintenance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchMaintenance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        maintenance.setId(longCount.incrementAndGet());

        // Create the Maintenance
        MaintenanceDTO maintenanceDTO = maintenanceMapper.toDto(maintenance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMaintenanceMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(maintenanceDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Maintenance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamMaintenance() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        maintenance.setId(longCount.incrementAndGet());

        // Create the Maintenance
        MaintenanceDTO maintenanceDTO = maintenanceMapper.toDto(maintenance);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restMaintenanceMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(maintenanceDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Maintenance in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteMaintenance() throws Exception {
        // Initialize the database
        insertedMaintenance = maintenanceRepository.saveAndFlush(maintenance);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the maintenance
        restMaintenanceMockMvc
            .perform(delete(ENTITY_API_URL_ID, maintenance.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return maintenanceRepository.count();
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

    protected Maintenance getPersistedMaintenance(Maintenance maintenance) {
        return maintenanceRepository.findById(maintenance.getId()).orElseThrow();
    }

    protected void assertPersistedMaintenanceToMatchAllProperties(Maintenance expectedMaintenance) {
        assertMaintenanceAllPropertiesEquals(expectedMaintenance, getPersistedMaintenance(expectedMaintenance));
    }

    protected void assertPersistedMaintenanceToMatchUpdatableProperties(Maintenance expectedMaintenance) {
        assertMaintenanceAllUpdatablePropertiesEquals(expectedMaintenance, getPersistedMaintenance(expectedMaintenance));
    }
}
