package com.dgi.gestionactifs.web.rest;

import static com.dgi.gestionactifs.domain.ContratAsserts.*;
import static com.dgi.gestionactifs.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.dgi.gestionactifs.IntegrationTest;
import com.dgi.gestionactifs.domain.Contrat;
import com.dgi.gestionactifs.domain.Fournisseur;
import com.dgi.gestionactifs.domain.enumeration.TypeContrat;
import com.dgi.gestionactifs.repository.ContratRepository;
import com.dgi.gestionactifs.security.AuthoritiesConstants;
import com.dgi.gestionactifs.service.dto.ContratDTO;
import com.dgi.gestionactifs.service.mapper.ContratMapper;
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
 * Integration tests for the {@link ContratResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class ContratResourceIT {

    private static final TypeContrat DEFAULT_TYPE_CONTRAT = TypeContrat.GARANTIE;
    private static final TypeContrat UPDATED_TYPE_CONTRAT = TypeContrat.MAINTENANCE;

    private static final String DEFAULT_REFERENCE = "AAAAAAAAAA";
    private static final String UPDATED_REFERENCE = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_DATE_DEBUT = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE_DEBUT = LocalDate.parse("2026-09-02");

    private static final LocalDate DEFAULT_DATE_FIN = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE_FIN = LocalDate.parse("2026-09-02");

    private static final String ENTITY_API_URL = "/api/contrats";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private ContratRepository contratRepository;

    @Autowired
    private ContratMapper contratMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restContratMockMvc;

    private Contrat contrat;

    private Contrat insertedContrat;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Contrat createEntity(EntityManager em) {
        Contrat contrat = new Contrat()
            .typeContrat(DEFAULT_TYPE_CONTRAT)
            .reference(DEFAULT_REFERENCE)
            .dateDebut(DEFAULT_DATE_DEBUT)
            .dateFin(DEFAULT_DATE_FIN);
        // Add required entity
        Fournisseur fournisseur;
        if (TestUtil.findAll(em, Fournisseur.class).isEmpty()) {
            fournisseur = FournisseurResourceIT.createEntity();
            em.persist(fournisseur);
            em.flush();
        } else {
            fournisseur = TestUtil.findAll(em, Fournisseur.class).get(0);
        }
        contrat.setFournisseur(fournisseur);
        return contrat;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Contrat createUpdatedEntity(EntityManager em) {
        Contrat updatedContrat = new Contrat()
            .typeContrat(UPDATED_TYPE_CONTRAT)
            .reference(UPDATED_REFERENCE)
            .dateDebut(UPDATED_DATE_DEBUT)
            .dateFin(UPDATED_DATE_FIN);
        // Add required entity
        Fournisseur fournisseur;
        if (TestUtil.findAll(em, Fournisseur.class).isEmpty()) {
            fournisseur = FournisseurResourceIT.createUpdatedEntity();
            em.persist(fournisseur);
            em.flush();
        } else {
            fournisseur = TestUtil.findAll(em, Fournisseur.class).get(0);
        }
        updatedContrat.setFournisseur(fournisseur);
        return updatedContrat;
    }

    @BeforeEach
    void initTest() {
        contrat = createEntity(em);
    }

    @AfterEach
    void cleanup() {
        if (insertedContrat != null) {
            contratRepository.delete(insertedContrat);
            insertedContrat = null;
        }
    }

    @Test
    @Transactional
    void createContrat() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Contrat
        ContratDTO contratDTO = contratMapper.toDto(contrat);
        var returnedContratDTO = om.readValue(
            restContratMockMvc
                .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(contratDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            ContratDTO.class
        );

        // Validate the Contrat in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        var returnedContrat = contratMapper.toEntity(returnedContratDTO);
        assertContratUpdatableFieldsEquals(returnedContrat, getPersistedContrat(returnedContrat));

        insertedContrat = returnedContrat;
    }

    @Test
    @Transactional
    void createContratWithExistingId() throws Exception {
        // Create the Contrat with an existing ID
        contrat.setId(1L);
        ContratDTO contratDTO = contratMapper.toDto(contrat);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        restContratMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(contratDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Contrat in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void checkTypeContratIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        contrat.setTypeContrat(null);

        // Create the Contrat, which fails.
        ContratDTO contratDTO = contratMapper.toDto(contrat);

        restContratMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(contratDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void checkDateFinIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        contrat.setDateFin(null);

        // Create the Contrat, which fails.
        ContratDTO contratDTO = contratMapper.toDto(contrat);

        restContratMockMvc
            .perform(post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(contratDTO)))
            .andExpect(status().isBadRequest());

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    void getAllContrats() throws Exception {
        // Initialize the database
        insertedContrat = contratRepository.saveAndFlush(contrat);

        // Get all the contratList
        restContratMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(contrat.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeContrat").value(hasItem(DEFAULT_TYPE_CONTRAT.toString())))
            .andExpect(jsonPath("$.[*].reference").value(hasItem(DEFAULT_REFERENCE)))
            .andExpect(jsonPath("$.[*].dateDebut").value(hasItem(DEFAULT_DATE_DEBUT.toString())))
            .andExpect(jsonPath("$.[*].dateFin").value(hasItem(DEFAULT_DATE_FIN.toString())));
    }

    @Test
    @Transactional
    void getContrat() throws Exception {
        // Initialize the database
        insertedContrat = contratRepository.saveAndFlush(contrat);

        // Get the contrat
        restContratMockMvc
            .perform(get(ENTITY_API_URL_ID, contrat.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(contrat.getId().intValue()))
            .andExpect(jsonPath("$.typeContrat").value(DEFAULT_TYPE_CONTRAT.toString()))
            .andExpect(jsonPath("$.reference").value(DEFAULT_REFERENCE))
            .andExpect(jsonPath("$.dateDebut").value(DEFAULT_DATE_DEBUT.toString()))
            .andExpect(jsonPath("$.dateFin").value(DEFAULT_DATE_FIN.toString()));
    }

    @Test
    @Transactional
    void getNonExistingContrat() throws Exception {
        // Get the contrat
        restContratMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putExistingContrat() throws Exception {
        // Initialize the database
        insertedContrat = contratRepository.saveAndFlush(contrat);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the contrat
        Contrat updatedContrat = contratRepository.findById(contrat.getId()).orElseThrow();
        // Disconnect from session so that the updates on updatedContrat are not directly saved in db
        em.detach(updatedContrat);
        updatedContrat
            .typeContrat(UPDATED_TYPE_CONTRAT)
            .reference(UPDATED_REFERENCE)
            .dateDebut(UPDATED_DATE_DEBUT)
            .dateFin(UPDATED_DATE_FIN);
        ContratDTO contratDTO = contratMapper.toDto(updatedContrat);

        restContratMockMvc
            .perform(
                put(ENTITY_API_URL_ID, contratDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(contratDTO))
            )
            .andExpect(status().isOk());

        // Validate the Contrat in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedContratToMatchAllProperties(updatedContrat);
    }

    @Test
    @Transactional
    void putNonExistingContrat() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contrat.setId(longCount.incrementAndGet());

        // Create the Contrat
        ContratDTO contratDTO = contratMapper.toDto(contrat);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restContratMockMvc
            .perform(
                put(ENTITY_API_URL_ID, contratDTO.getId()).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(contratDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Contrat in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchContrat() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contrat.setId(longCount.incrementAndGet());

        // Create the Contrat
        ContratDTO contratDTO = contratMapper.toDto(contrat);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restContratMockMvc
            .perform(
                put(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(contratDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Contrat in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamContrat() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contrat.setId(longCount.incrementAndGet());

        // Create the Contrat
        ContratDTO contratDTO = contratMapper.toDto(contrat);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restContratMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsBytes(contratDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Contrat in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateContratWithPatch() throws Exception {
        // Initialize the database
        insertedContrat = contratRepository.saveAndFlush(contrat);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the contrat using partial update
        Contrat partialUpdatedContrat = new Contrat();
        partialUpdatedContrat.setId(contrat.getId());

        partialUpdatedContrat.dateDebut(UPDATED_DATE_DEBUT).dateFin(UPDATED_DATE_FIN);

        restContratMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedContrat.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedContrat))
            )
            .andExpect(status().isOk());

        // Validate the Contrat in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertContratUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedContrat, contrat), getPersistedContrat(contrat));
    }

    @Test
    @Transactional
    void fullUpdateContratWithPatch() throws Exception {
        // Initialize the database
        insertedContrat = contratRepository.saveAndFlush(contrat);

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the contrat using partial update
        Contrat partialUpdatedContrat = new Contrat();
        partialUpdatedContrat.setId(contrat.getId());

        partialUpdatedContrat
            .typeContrat(UPDATED_TYPE_CONTRAT)
            .reference(UPDATED_REFERENCE)
            .dateDebut(UPDATED_DATE_DEBUT)
            .dateFin(UPDATED_DATE_FIN);

        restContratMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedContrat.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(partialUpdatedContrat))
            )
            .andExpect(status().isOk());

        // Validate the Contrat in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertContratUpdatableFieldsEquals(partialUpdatedContrat, getPersistedContrat(partialUpdatedContrat));
    }

    @Test
    @Transactional
    void patchNonExistingContrat() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contrat.setId(longCount.incrementAndGet());

        // Create the Contrat
        ContratDTO contratDTO = contratMapper.toDto(contrat);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restContratMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, contratDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(contratDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Contrat in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchContrat() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contrat.setId(longCount.incrementAndGet());

        // Create the Contrat
        ContratDTO contratDTO = contratMapper.toDto(contrat);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restContratMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, longCount.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(om.writeValueAsBytes(contratDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the Contrat in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamContrat() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        contrat.setId(longCount.incrementAndGet());

        // Create the Contrat
        ContratDTO contratDTO = contratMapper.toDto(contrat);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restContratMockMvc
            .perform(patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(om.writeValueAsBytes(contratDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the Contrat in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteContrat() throws Exception {
        // Initialize the database
        insertedContrat = contratRepository.saveAndFlush(contrat);

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the contrat
        restContratMockMvc
            .perform(delete(ENTITY_API_URL_ID, contrat.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return contratRepository.count();
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

    protected Contrat getPersistedContrat(Contrat contrat) {
        return contratRepository.findById(contrat.getId()).orElseThrow();
    }

    protected void assertPersistedContratToMatchAllProperties(Contrat expectedContrat) {
        assertContratAllPropertiesEquals(expectedContrat, getPersistedContrat(expectedContrat));
    }

    protected void assertPersistedContratToMatchUpdatableProperties(Contrat expectedContrat) {
        assertContratAllUpdatablePropertiesEquals(expectedContrat, getPersistedContrat(expectedContrat));
    }
}
