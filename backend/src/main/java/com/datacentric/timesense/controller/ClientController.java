package com.datacentric.timesense.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datacentric.timesense.model.Client;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.ClientRepository;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.rest.I18nResponses;
import com.datacentric.utils.rest.JsonViewPage;
import com.datacentric.utils.rest.RestUtils;
import com.fasterxml.jackson.annotation.JsonView;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    public static final class Views {
        interface GetBasicInfo extends User.Views.Basic {
        }

        public interface GetClients extends Client.Views.Public,
                JsonViewPage.Views.Public {
        }

        public interface GetClient extends Client.Views.Complete,
                JsonViewPage.Views.Public {
        }
    }

    private ClientRepository clientRepository;
    private SecurityUtils securityUtils;
    private UserUtils userUtils;

    @Autowired
    public ClientController(ClientRepository clientRepository,
            SecurityUtils securityUtils, UserUtils userUtils) {
        this.clientRepository = clientRepository;
        this.securityUtils = securityUtils;
        this.userUtils = userUtils;
    }

    private static final int DEFAULT_FIRST_ROW = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String DEFAULT_FIRST_ROW_STR = "0";
    private static final String DEFAULT_PAGE_SIZE_STR = "10";
    private static final String REQUIRED_PERMISSION = "CREATE_PROJECTS";

    @JsonView(Views.GetClients.class)
    @GetMapping
    public JsonViewPage<Client> getClients(
            @RequestParam(defaultValue = DEFAULT_FIRST_ROW_STR, required = false) int firstRow,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE_STR, required = false) int numRows,
            @RequestParam(defaultValue = "id", required = false) String sort,
            @RequestParam(defaultValue = "", required = false) String filter) {

        Pageable pageable = PageRequest.of(Math.max(firstRow, DEFAULT_FIRST_ROW),
                Math.max(Math.min(numRows, DEFAULT_PAGE_SIZE), 1),
                RestUtils.getSortFromString(sort));

        Specification<Client> spec = Specification.where(null);

        if (!filter.isEmpty()) {
            Specification<Client> filterSpec =
                RestUtils.getSpecificationFromFilter("basic", filter);
            spec = spec.and(filterSpec);
        }

        if (spec != null) {
            return new JsonViewPage<>(clientRepository.findAll(spec, pageable));
        } else {
            return new JsonViewPage<>(clientRepository.findAll(pageable));
        }
    }

    @JsonView(Views.GetClient.class)
    @GetMapping("/{id}")
    public ResponseEntity<?> getGetClientById(@PathVariable Long id) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);

            if (!hasPermission) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            Client client = clientRepository.findById(id).orElse(null);
            if (client == null) {
                return I18nResponses.notFound(MessagesCodes.CLIENT_NOT_FOUND);
            }

            return ResponseEntity.ok(client);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (NumberFormatException e) {
            return I18nResponses.notFound(MessagesCodes.CLIENT_NOT_FOUND);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetClient.class)
    @PostMapping()
    public ResponseEntity<?> createClient(@RequestBody Client client) {
        try {
            UserSecurityData user = userUtils.getOrCreateUser();
            boolean hasPermission = securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION);

            if (!hasPermission) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            if (clientRepository.existsByName(client.getName())) {
                return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
            }
            Client newClient = clientRepository.save(client);
            return I18nResponses.httpResponseWithData(HttpStatus.CREATED,
                    MessagesCodes.CLIENT_CREATED_OK,
                    newClient);
        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (HttpMessageNotReadableException e) {
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClient(@PathVariable Long id) {
        try {
            Optional<Client> result = clientRepository.findById(id);
            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.CLIENT_NOT_FOUND);
            }

            UserSecurityData user = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            clientRepository.deleteClientById(id);
            return I18nResponses.httpResponse(HttpStatus.ACCEPTED,
                    MessagesCodes.CLIENT_DELETED_OK);

        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

    @JsonView(Views.GetClient.class)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClusterId(@PathVariable Long id,
            @RequestBody Client newClient) {
        try {
            Optional<Client> result = clientRepository.findById(id);

            if (result.isEmpty()) {
                return I18nResponses.notFound(MessagesCodes.CLIENT_NOT_FOUND);
            }

            UserSecurityData user = userUtils.getOrCreateUser();

            if (!securityUtils.hasSystemPermission(user, REQUIRED_PERMISSION)) {
                return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
            }

            Client client = result.get();
            if (!client.getName().equals(newClient.getName())) {
                if (clientRepository.existsByName(newClient.getName())) {
                    return I18nResponses.badRequest(MessagesCodes.UNIQUE_NAME_VIOLATION);
                }
            }

            client.setName(newClient.getName());
            client.setClientTicker(newClient.getClientTicker());

            Client updatedClient = clientRepository.save(client);
            return I18nResponses.httpResponseWithData(HttpStatus.ACCEPTED,
                    MessagesCodes.CLIENT_UPDATED_OK,
                    updatedClient);

        } catch (AuthenticationException e) {
            return I18nResponses.httpResponse(HttpStatus.UNAUTHORIZED,
                    MessagesCodes.UNAUTHORIZED_USER);
        } catch (HttpMessageNotReadableException e) {
            return I18nResponses.badRequest(MessagesCodes.MALFORMED_REQUEST_BODY);
        } catch (DataIntegrityViolationException e) {
            return I18nResponses.badRequest(MessagesCodes.DATA_INTEGRITY_VIOLATION);
        } catch (Exception e) {
            return I18nResponses.httpResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    MessagesCodes.INTERNAL_SERVER_ERROR);
        }
    }

}
