package com.dgi.gestionactifs.web.rest;

import com.dgi.gestionactifs.repository.AffectationRepository;
import com.dgi.gestionactifs.security.SecurityUtils;
import com.dgi.gestionactifs.service.AffectationQueryService;
import com.dgi.gestionactifs.service.AffectationService;
import com.dgi.gestionactifs.service.criteria.AffectationCriteria;
import com.dgi.gestionactifs.service.dto.AffectationDTO;
import com.dgi.gestionactifs.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.service.filter.LongFilter;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.dgi.gestionactifs.domain.Affectation}.
 */
@RestController
@RequestMapping("/api/affectations")
public class AffectationResource {

    private static final Logger LOG = LoggerFactory.getLogger(AffectationResource.class);

    private static final String ENTITY_NAME = "affectation";

    @Value("${jhipster.clientApp.name:gestionActifsDgi}")
    private String applicationName;

    private final AffectationService affectationService;

    private final AffectationRepository affectationRepository;

    private final AffectationQueryService affectationQueryService;

    public AffectationResource(
        AffectationService affectationService,
        AffectationRepository affectationRepository,
        AffectationQueryService affectationQueryService
    ) {
        this.affectationService = affectationService;
        this.affectationRepository = affectationRepository;
        this.affectationQueryService = affectationQueryService;
    }

    /**
     * {@code POST  /affectations} : Create a new affectation.
     *
     * @param affectationDTO the affectationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new affectationDTO, or with status {@code 400 (Bad Request)} if the affectation has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE')")
    @PostMapping("")
    public ResponseEntity<AffectationDTO> createAffectation(@Valid @RequestBody AffectationDTO affectationDTO) throws URISyntaxException {
        LOG.debug("REST request to save Affectation : {}", affectationDTO);
        if (affectationDTO.getId() != null) {
            throw new BadRequestAlertException("A new affectation cannot already have an ID", ENTITY_NAME, "idexists");
        }
        affectationDTO = affectationService.save(affectationDTO);
        return ResponseEntity.created(new URI("/api/affectations/" + affectationDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, affectationDTO.getId().toString()))
            .body(affectationDTO);
    }

    /**
     * {@code PUT  /affectations/:id} : Updates an existing affectation.
     *
     * @param id the id of the affectationDTO to save.
     * @param affectationDTO the affectationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated affectationDTO,
     * or with status {@code 400 (Bad Request)} if the affectationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the affectationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE')")
    @PutMapping("/{id}")
    public ResponseEntity<AffectationDTO> updateAffectation(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody AffectationDTO affectationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Affectation : {}, {}", id, affectationDTO);
        if (affectationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, affectationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!affectationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        affectationDTO = affectationService.update(affectationDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, affectationDTO.getId().toString()))
            .body(affectationDTO);
    }

    /**
     * {@code PATCH  /affectations/:id} : Partial updates given fields of an existing affectation, field will ignore if it is null
     *
     * @param id the id of the affectationDTO to save.
     * @param affectationDTO the affectationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated affectationDTO,
     * or with status {@code 400 (Bad Request)} if the affectationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the affectationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the affectationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AffectationDTO> partialUpdateAffectation(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody AffectationDTO affectationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Affectation partially : {}, {}", id, affectationDTO);
        if (affectationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, affectationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!affectationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AffectationDTO> result = affectationService.partialUpdate(affectationDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, affectationDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /affectations} : get all the Affectations.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Affectations in body.
     */
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE') or hasAuthority('ROLE_AGENT')"
    )
    @GetMapping("")
    public ResponseEntity<List<AffectationDTO>> getAllAffectations(
        AffectationCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Affectations by criteria: {}", criteria);

        boolean isAgentOnly =
            SecurityUtils.hasCurrentUserThisAuthority("ROLE_AGENT") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_TECHNICIEN") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_RESPONSABLE");

        if (isAgentOnly) {
            java.util.List<Long> affectationIds = affectationRepository
                .findByUtilisateur_Login(SecurityUtils.getCurrentUserLogin().orElse(""))
                .stream()
                .map(com.dgi.gestionactifs.domain.Affectation::getId)
                .distinct()
                .toList();
            if (affectationIds.isEmpty()) {
                HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
                    ServletUriComponentsBuilder.fromCurrentRequest(),
                    Page.empty(pageable)
                );
                return ResponseEntity.ok().headers(headers).body(List.of());
            }
            LongFilter idFilter = new LongFilter();
            idFilter.setIn(affectationIds);
            criteria.setId(idFilter);
        }

        Page<AffectationDTO> page = affectationQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /affectations/count} : count all the affectations.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countAffectations(AffectationCriteria criteria) {
        LOG.debug("REST request to count Affectations by criteria: {}", criteria);
        return ResponseEntity.ok().body(affectationQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /affectations/:id} : get the "id" affectation.
     *
     * @param id the id of the affectationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the affectationDTO, or with status {@code 404 (Not Found)}.
     */
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE') or hasAuthority('ROLE_AGENT')"
    )
    @GetMapping("/{id}")
    public ResponseEntity<AffectationDTO> getAffectation(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Affectation : {}", id);
        Optional<AffectationDTO> affectationDTO = affectationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(affectationDTO);
    }

    /**
     * {@code DELETE  /affectations/:id} : delete the "id" affectation.
     *
     * @param id the id of the affectationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAffectation(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Affectation : {}", id);
        affectationService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
