package com.datacentric.timesense.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datacentric.timesense.model.ResourcePermission;
import static com.datacentric.timesense.model.SystemAccessTypes.MANAGE_SECURITY;
import com.datacentric.timesense.model.User;
import com.datacentric.timesense.repository.ResourcePermissionRepository;
import com.datacentric.timesense.repository.ResourcePermissionRepository.ResourcePermissionWithSubjectName;
import com.datacentric.timesense.utils.SecurityUtils;
import com.datacentric.timesense.utils.i18n.MessagesCodes;
import com.datacentric.timesense.utils.rest.UserUtils;
import com.datacentric.timesense.utils.security.UserSecurityData;
import com.datacentric.utils.rest.I18nResponses;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@RestController
@RequestMapping("/api/permissions")
public class ResourcePermissionController {

    private ResourcePermissionRepository permissionsRepository;
    private UserUtils userUtils;
    private SecurityUtils securityUtils;

    interface Views {
        interface GetResourcePermissions extends ResourcePermission.Views.Minimal {
        }
    }

    @Autowired
    public ResourcePermissionController(ResourcePermissionRepository permissionsRepository,
            UserUtils userUtils, SecurityUtils securityUtils) {
        this.permissionsRepository = permissionsRepository;
        this.userUtils = userUtils;
        this.securityUtils = securityUtils;
    }

    /**
     * Get the permissions for a given subject.
     */
    @GetMapping("/{resourceType}/{resourceId}")
    public final ResponseEntity<PermissionsMap> getPermissions(@PathVariable String resourceType,
            @PathVariable Long resourceId,
            @RequestParam(name = "user", defaultValue = "") String userFilter) {

        List<ResourcePermissionWithSubjectName> permissions = permissionsRepository
                .findByResourceTypeAndResourceIdWithSubjectName(resourceType, resourceId);
        return ResponseEntity.ok(new PermissionsMap(permissions));
    }

    /**
     * Get all the user permissions.
     */
    @JsonView(Views.GetResourcePermissions.class)
    @GetMapping("/user")
    public final ResponseEntity<List<ResourcePermission>> getUserPermissions() {

        UserSecurityData user = userUtils.getOrCreateUser();

        List<ResourcePermission> userResourcePermissions = permissionsRepository.getUserPermissions(
                user.getId(),
                user.getRoles(), user.getUserGroups());

        Map<String, ResourcePermission> uniquePermissions = userResourcePermissions.stream()
                .collect(Collectors.toMap(
                        resourcePermission -> resourcePermission.getAccessType() + "-"
                                + resourcePermission.getResourceId(),
                        resourcePermission -> resourcePermission,
                        // in case of duplicate keep the first one
                        (existing, replacement) -> existing));

        return ResponseEntity.ok(uniquePermissions.values().stream().collect(Collectors.toList()));

    }

    /**
     * Get the permissions for a given subject.
     */
    @GetMapping("/{resourceType}/{resourceId}/user")
    public final ResponseEntity<List<String>> getPermissions(@PathVariable String resourceType,
            @PathVariable Long resourceId) {

        UserSecurityData user = userUtils.getOrCreateUser();

        List<String> allowedAccesses = permissionsRepository.getAllowedPermissions(
                resourceType, resourceId, user.getId(), user.getRoles(), user.getUserGroups());

        return ResponseEntity.ok(allowedAccesses);
    }

    @PostMapping("/{resourceType}/{resourceId}")
    public final ResponseEntity<?> addPermission(@PathVariable String resourceType,
            @PathVariable Long resourceId, @RequestBody PermissionsMap permissionsMap) {

        UserSecurityData user = userUtils.getOrCreateUser();
        if (!securityUtils.hasSystemPermission(user, MANAGE_SECURITY)) {
            return I18nResponses.forbidden(MessagesCodes.PERMISSIONS_DENIED);
        }
        List<ResourcePermission> permList = permissionsMap.toList(resourceType,
                resourceId, user.toUserPlaceholder());

        // TODO: Improve the handling of the permissions to only delete the permissions
        // that are removed
        permissionsRepository.deleteByResourceTypeAndResourceId(resourceType, resourceId);
        permissionsRepository.saveAll(permList);
        return I18nResponses.httpResponse(HttpStatus.OK, MessagesCodes.PERMISSIONS_SAVED);
    }

    /**
     * Holds the permissions for a single onject in a structure more appropriate
     * to send to the clients through the API.
     */
    @JsonDeserialize(using = PermissionsMapDeserializer.class)
    @JsonSerialize(using = PermissionsMapSerializer.class)
    public static final class PermissionsMap {
        public Map<String, Map<String, List<Subject>>> permissions = new HashMap<>();

        public PermissionsMap() {
        }

        /**
         * NOTE: the boolean argument is just to make sure that the deserializer
         * doesn't confuse the two constructors.
         */
        public PermissionsMap(Map<String, Map<String, List<Subject>>> permissions, boolean dummy) {
            this.permissions = permissions;
        }

        /**
         * Creates a Permissions Map based on a list of resource permissions.
         * It expects all of the permissions to belong to the same resource.
         */
        public PermissionsMap(List<ResourcePermissionWithSubjectName> permissions) {
            String resourceType = null;
            Long resourceId = null;

            for (ResourcePermissionWithSubjectName permission : permissions) {

                assert resourceType == null || resourceType.equals(permission.getResourceType());
                assert resourceId == null || resourceId.equals(permission.getResourceId());

                resourceType = permission.getResourceType();
                resourceId = permission.getResourceId();

                List<Subject> subjectList = this.permissions
                        .computeIfAbsent(permission.getAccessType(), k -> new HashMap<>())
                        .computeIfAbsent(permission.getSubjectType(), k -> new ArrayList<>());
                subjectList.add(
                        new Subject(permission.getSubjectId(), permission.getSubjectName()));
            }
        }

        /**
         * Converts the PermissionsMap to a list of ResourcePermissions.
         */
        public List<ResourcePermission> toList(String resourceType, Long resourceId, User user) {
            List<ResourcePermission> result = new ArrayList<>();
            for (Map.Entry<String, Map<String, List<Subject>>> entry : permissions.entrySet()) {
                String accessType = entry.getKey();
                for (Map.Entry<String, List<Subject>> entry2 : entry.getValue().entrySet()) {
                    String subjectType = entry2.getKey();
                    for (Subject subject : entry2.getValue()) {
                        result.add(new ResourcePermission(resourceType, resourceId, accessType,
                                subjectType, Long.valueOf(subject.getId()), user));
                    }
                }
            }
            return result;
        }
    }

    public static final class Subject {
        private long id;
        private String name;

        Subject(long id, String name) {
            this.id = id;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static final class PermissionsMapDeserializer extends JsonDeserializer<PermissionsMap> {
        @Override
        public PermissionsMap deserialize(JsonParser p, DeserializationContext ctxt)
                throws IOException {
            return new PermissionsMap(
                    p.readValueAs(
                            new TypeReference<Map<String, Map<String, List<Subject>>>>() {
                            }),
                    false);
        }
    }

    public static final class PermissionsMapSerializer
            extends com.fasterxml.jackson.databind.JsonSerializer<PermissionsMap> {
        @Override
        public void serialize(PermissionsMap value, com.fasterxml.jackson.core.JsonGenerator gen,
                com.fasterxml.jackson.databind.SerializerProvider serializers)
                throws IOException {
            gen.writeObject(value.permissions);
        }
    }
}
