package com.dgi.gestionactifs.web.rest;

import com.dgi.gestionactifs.repository.ActifRepository;
import com.dgi.gestionactifs.repository.AffectationRepository;
import com.dgi.gestionactifs.security.SecurityUtils;
import com.dgi.gestionactifs.service.ActifQueryService;
import com.dgi.gestionactifs.service.ActifService;
import com.dgi.gestionactifs.service.criteria.ActifCriteria;
import com.dgi.gestionactifs.service.dto.ActifDTO;
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
 * REST controller for managing {@link com.dgi.gestionactifs.domain.Actif}.
 */
@RestController
@RequestMapping("/api/actifs")
public class ActifResource {

    private static final Logger LOG = LoggerFactory.getLogger(ActifResource.class);

    private static final String ENTITY_NAME = "actif";

    @Value("${jhipster.clientApp.name:gestionActifsDgi}")
    private String applicationName;

    private final ActifService actifService;

    private final ActifRepository actifRepository;

    private final ActifQueryService actifQueryService;

    private final AffectationRepository affectationRepository;

    public ActifResource(
        ActifService actifService,
        ActifRepository actifRepository,
        ActifQueryService actifQueryService,
        AffectationRepository affectationRepository
    ) {
        this.actifService = actifService;
        this.actifRepository = actifRepository;
        this.actifQueryService = actifQueryService;
        this.affectationRepository = affectationRepository;
    }

    /**
     * {@code POST  /actifs} : Create a new actif.
     *
     * @param actifDTO the actifDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new actifDTO, or with status {@code 400 (Bad Request)} if the actif has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN')")
    @PostMapping("")
    public ResponseEntity<ActifDTO> createActif(@Valid @RequestBody ActifDTO actifDTO) throws URISyntaxException {
        LOG.debug("REST request to save Actif : {}", actifDTO);
        if (actifDTO.getId() != null) {
            throw new BadRequestAlertException("A new actif cannot already have an ID", ENTITY_NAME, "idexists");
        }
        actifDTO = actifService.save(actifDTO);
        return ResponseEntity.created(new URI("/api/actifs/" + actifDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, actifDTO.getId().toString()))
            .body(actifDTO);
    }

    /**
     * {@code PUT  /actifs/:id} : Updates an existing actif.
     *
     * @param id the id of the actifDTO to save.
     * @param actifDTO the actifDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated actifDTO,
     * or with status {@code 400 (Bad Request)} if the actifDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the actifDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN')")
    @PutMapping("/{id}")
    public ResponseEntity<ActifDTO> updateActif(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody ActifDTO actifDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Actif : {}, {}", id, actifDTO);
        if (actifDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, actifDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!actifRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        actifDTO = actifService.update(actifDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, actifDTO.getId().toString()))
            .body(actifDTO);
    }

    /**
     * {@code PATCH  /actifs/:id} : Partial updates given fields of an existing actif, field will ignore if it is null
     *
     * @param id the id of the actifDTO to save.
     * @param actifDTO the actifDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated actifDTO,
     * or with status {@code 400 (Bad Request)} if the actifDTO is not valid,
     * or with status {@code 404 (Not Found)} if the actifDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the actifDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ActifDTO> partialUpdateActif(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody ActifDTO actifDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Actif partially : {}, {}", id, actifDTO);
        if (actifDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, actifDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!actifRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ActifDTO> result = actifService.partialUpdate(actifDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, actifDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /actifs} : get all the Actifs.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Actifs in body.
     */
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE') or hasAuthority('ROLE_AGENT')"
    )
    @GetMapping("")
    public ResponseEntity<List<ActifDTO>> getAllActifs(
        ActifCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Actifs by criteria: {}", criteria);

        boolean isAgentOnly =
            SecurityUtils.hasCurrentUserThisAuthority("ROLE_AGENT") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_TECHNICIEN") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_RESPONSABLE");

        if (isAgentOnly) {
            List<Long> actifIds = affectationRepository
                .findByUtilisateur_LoginAndDateRestitutionIsNull(SecurityUtils.getCurrentUserLogin().orElse(""))
                .stream()
                .map(affectation -> affectation.getActif().getId())
                .distinct()
                .toList();
            if (actifIds.isEmpty()) {
                HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
                    ServletUriComponentsBuilder.fromCurrentRequest(),
                    Page.empty(pageable)
                );
                return ResponseEntity.ok().headers(headers).body(List.of());
            }
            LongFilter idFilter = new LongFilter();
            idFilter.setIn(actifIds);
            criteria.setId(idFilter);
        }

        Page<ActifDTO> page = actifQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /actifs/count} : count all the actifs.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countActifs(ActifCriteria criteria) {
        LOG.debug("REST request to count Actifs by criteria: {}", criteria);
        boolean isAgentOnlyCount =
            SecurityUtils.hasCurrentUserThisAuthority("ROLE_AGENT") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_TECHNICIEN") &&
            !SecurityUtils.hasCurrentUserThisAuthority("ROLE_RESPONSABLE");
        if (isAgentOnlyCount) {
            List<Long> actifIds = affectationRepository
                .findByUtilisateur_LoginAndDateRestitutionIsNull(SecurityUtils.getCurrentUserLogin().orElse(""))
                .stream()
                .map(affectation -> affectation.getActif().getId())
                .distinct()
                .toList();
            if (actifIds.isEmpty()) {
                return ResponseEntity.ok().body(0L);
            }
            LongFilter idFilter = new LongFilter();
            idFilter.setIn(actifIds);
            criteria.setId(idFilter);
        }
        return ResponseEntity.ok().body(actifQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /actifs/:id} : get the "id" actif.
     *
     * @param id the id of the actifDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the actifDTO, or with status {@code 404 (Not Found)}.
     */
    @PreAuthorize(
        "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TECHNICIEN') or hasAuthority('ROLE_RESPONSABLE') or hasAuthority('ROLE_AGENT')"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ActifDTO> getActif(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Actif : {}", id);
        Optional<ActifDTO> actifDTO = actifService.findOne(id);
        return ResponseUtil.wrapOrNotFound(actifDTO);
    }

    /**
     * {@code DELETE  /actifs/:id} : delete the "id" actif.
     *
     * @param id the id of the actifDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActif(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Actif : {}", id);
        actifService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
