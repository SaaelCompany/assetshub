package com.assetstree.jira.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.CrowdDirectoryService;
import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.Directory;
import com.atlassian.crowd.embedded.api.UserWithAttributes;
import com.atlassian.jira.bc.user.search.UserSearchParams;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.mail.Email;
import com.atlassian.mail.queue.MailQueue;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.assetstree.jira.PluginInfo;
import com.assetstree.jira.ao.AssetActivityEntity;
import com.assetstree.jira.ao.AssetAttributeEntity;
import com.assetstree.jira.ao.AssetCommentEntity;
import com.assetstree.jira.ao.CardTemplateEntity;
import com.assetstree.jira.ao.AssetCounterEntity;
import com.assetstree.jira.ao.AssetEntity;
import com.assetstree.jira.ao.AssetFieldEntity;
import com.assetstree.jira.ao.FlowGraphEntity;
import com.assetstree.jira.ao.FlowRuleEntity;
import com.assetstree.jira.ao.AssetFileEntity;
import com.assetstree.jira.ao.AssetIssueLinkEntity;
import com.assetstree.jira.ao.InventoryMarkEntity;
import com.assetstree.jira.ao.PortalRuleConditionEntity;
import com.assetstree.jira.ao.PortalRuleEntity;
import com.assetstree.jira.ao.ProjectAssetKeyEntity;
import com.assetstree.jira.ao.ProjectGrantEntity;
import com.assetstree.jira.ao.ProjectStatusEntity;
import com.assetstree.jira.ao.ProjectViewEntity;
import com.assetstree.jira.ao.FloorEntity;
import com.assetstree.jira.ao.LifecycleEntity;
import com.assetstree.jira.ao.PinEntity;
import com.assetstree.jira.ao.RoomEntity;
import com.assetstree.jira.ao.SavedReportEntity;
import com.assetstree.jira.ao.ServicePlanEntity;
import com.assetstree.jira.ao.StaffDutyEntity;
import com.assetstree.jira.ao.AssetTypeEntity;
import com.assetstree.jira.dto.ActivityDto;
import com.assetstree.jira.dto.AssetDto;
import com.assetstree.jira.dto.AssetKeyDto;
import com.assetstree.jira.dto.FieldOrderDto;
import com.assetstree.jira.dto.ProjectViewDto;
import com.assetstree.jira.model.ProjectModules;
import com.assetstree.jira.model.ViewColumns;
import com.assetstree.jira.dto.AssetListDto;
import com.assetstree.jira.dto.AssetTypeDto;
import com.assetstree.jira.dto.AttributeDto;
import com.assetstree.jira.dto.BulkIssueDto;
import com.assetstree.jira.dto.BulkResultDto;
import com.assetstree.jira.dto.CommentDto;
import com.assetstree.jira.dto.FieldDto;
import com.assetstree.jira.dto.FlowBookDto;
import com.assetstree.jira.dto.FlowGraphDto;
import com.assetstree.jira.dto.FlowNodeDto;
import com.assetstree.jira.dto.FlowProcessDto;
import com.assetstree.jira.dto.FlowRuleDto;
import com.assetstree.jira.dto.IssueTypeDto;
import com.assetstree.jira.dto.FileDto;
import com.assetstree.jira.dto.GrantDto;
import com.assetstree.jira.dto.ImportIssueDto;
import com.assetstree.jira.dto.ImportResultDto;
import com.assetstree.jira.dto.InventoryDto;
import com.assetstree.jira.dto.InventoryRowDto;
import com.assetstree.jira.dto.IssueContextDto;
import com.assetstree.jira.dto.IssueRefDto;
import com.assetstree.jira.dto.MetaDto;
import com.assetstree.jira.dto.PickerNodeDto;
import com.assetstree.jira.dto.PortalRuleDto;
import com.assetstree.jira.dto.ProjectDto;
import com.assetstree.jira.dto.ReportBucketDto;
import com.assetstree.jira.dto.ReportDto;
import com.assetstree.jira.dto.ReportHolderDto;
import com.assetstree.jira.dto.ReportPlaceDto;
import com.assetstree.jira.dto.FloorDto;
import com.assetstree.jira.dto.LifecycleDto;
import com.assetstree.jira.dto.PinDto;
import com.assetstree.jira.dto.ReportLineDto;
import com.assetstree.jira.dto.ReportViewDto;
import com.assetstree.jira.dto.RoomDto;
import com.assetstree.jira.dto.SavedReportDto;
import com.assetstree.jira.dto.ScheduleRowDto;
import com.assetstree.jira.dto.StaffDutyDto;
import com.assetstree.jira.dto.ServicePlanDto;
import com.assetstree.jira.dto.StatusDto;
import com.assetstree.jira.dto.UserProfileDto;
import com.assetstree.jira.model.AssetDraft;
import com.assetstree.jira.model.CopyNames;
import com.assetstree.jira.model.BulkDraft;
import com.assetstree.jira.model.CardCanvas;
import com.assetstree.jira.model.CardLayout;
import com.assetstree.jira.model.BulkSelection;
import com.assetstree.jira.model.AssetException;
import com.assetstree.jira.model.AssetValidator;
import com.assetstree.jira.model.AttributeDraft;
import com.assetstree.jira.model.DefaultTypes;
import com.assetstree.jira.model.EquipmentExchange;
import com.assetstree.jira.model.EquipmentSheet;
import com.assetstree.jira.model.ImportDraft;
import com.assetstree.jira.model.WorkbookSheet;
import com.assetstree.jira.model.FieldChoices;
import com.assetstree.jira.model.FieldDates;
import com.assetstree.jira.model.FieldDraft;
import com.assetstree.jira.model.FlowBook;
import com.assetstree.jira.model.FlowGraph;
import com.assetstree.jira.model.PickerLayout;
import com.assetstree.jira.model.PlaceArrangeDraft;
import com.assetstree.jira.model.PlaceLayout;
import com.assetstree.jira.model.FlowRuleDraft;
import com.assetstree.jira.model.FlowRules;
import com.assetstree.jira.model.FieldKinds;
import com.assetstree.jira.model.GrantCaps;
import com.assetstree.jira.model.GrantDraft;
import com.assetstree.jira.model.MoveDraft;
import com.assetstree.jira.model.AssetKeyRules;
import com.assetstree.jira.model.ObjectKeys;
import com.assetstree.jira.model.PlaceHistory;
import com.assetstree.jira.model.PortalConditionDraft;
import com.assetstree.jira.model.PortalRuleDraft;
import com.assetstree.jira.model.StatusDraft;
import com.assetstree.jira.model.ProjectKeys;
import com.assetstree.jira.model.StatusCategories;
import com.assetstree.jira.model.Depreciation;
import com.assetstree.jira.model.FloorBounds;
import com.assetstree.jira.model.ModuleSetup;
import com.assetstree.jira.model.LifecycleDraft;
import com.assetstree.jira.model.MoneyText;
import com.assetstree.jira.model.ReportCatalog;
import com.assetstree.jira.model.SavedReportDraft;
import com.assetstree.jira.model.ScheduleAnchor;
import com.assetstree.jira.model.ScheduleDraft;
import com.assetstree.jira.model.StaffDutyDraft;
import com.assetstree.jira.model.ServiceDue;
import com.assetstree.jira.model.WorkKind;
import com.assetstree.jira.model.WriteoffDraft;
import com.assetstree.jira.model.PlaceTypeDraft;
import com.assetstree.jira.model.ServicePlanDraft;
import com.assetstree.jira.model.Statuses;
import com.assetstree.jira.model.StatusSummary;
import com.assetstree.jira.model.TreeLogic;
import com.assetstree.jira.model.TypeDraft;
import com.assetstree.jira.model.TypeIcons;
import com.assetstree.jira.web.UiCatalog;
import net.java.ao.DBParam;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class AssetServiceImpl implements AssetService {
    private static final Logger log = LoggerFactory.getLogger(AssetServiceImpl.class);
    private static final int SEARCH_LIMIT = 30;
    private static final int ID_BATCH = 200;
    private static final ConcurrentHashMap<String, Object> KEY_LOCKS = new ConcurrentHashMap<String, Object>();
    private static final Pattern NUMBER = Pattern.compile("^-?\\d{1,12}(\\.\\d{1,4})?$");
    private static final GlobalPermissionKey MANAGE_ASSETS = GlobalPermissionKey.of("MANAGE_ASSETS");
    private static final GlobalPermissionKey MANAGE_ASSETS_FULL = GlobalPermissionKey.of(PluginInfo.KEY + ":MANAGE_ASSETS");
    private static final ProjectPermissionKey EDIT_ASSETS = new ProjectPermissionKey(PluginInfo.KEY + ":edit-assets");

    private volatile ActiveObjects aoRef;
    private volatile IssueManager issueManagerRef;
    private volatile JiraAuthenticationContext authenticationContextRef;
    private volatile GlobalPermissionManager globalPermissionManagerRef;
    private volatile PermissionManager permissionManagerRef;
    private volatile UserManager userManagerRef;
    private volatile ProjectManager projectManagerRef;
    private volatile UserSearchService userSearchServiceRef;
    private volatile CrowdService crowdServiceRef;
    private volatile CrowdDirectoryService crowdDirectoryServiceRef;
    private volatile GroupManager groupManagerRef;

    private ActiveObjects ao() {
        ActiveObjects current = aoRef;
        if (current == null) {
            current = JiraServices.activeObjects();
            aoRef = current;
        }
        return current;
    }

    private IssueManager issueManager() {
        IssueManager current = issueManagerRef;
        if (current == null) {
            current = ComponentAccessor.getIssueManager();
            issueManagerRef = current;
        }
        return current;
    }

    private JiraAuthenticationContext authenticationContext() {
        JiraAuthenticationContext current = authenticationContextRef;
        if (current == null) {
            current = ComponentAccessor.getJiraAuthenticationContext();
            authenticationContextRef = current;
        }
        return current;
    }

    private GlobalPermissionManager globalPermissionManager() {
        GlobalPermissionManager current = globalPermissionManagerRef;
        if (current == null) {
            current = ComponentAccessor.getGlobalPermissionManager();
            globalPermissionManagerRef = current;
        }
        return current;
    }

    private PermissionManager permissionManager() {
        PermissionManager current = permissionManagerRef;
        if (current == null) {
            current = ComponentAccessor.getPermissionManager();
            permissionManagerRef = current;
        }
        return current;
    }

    private UserManager userManager() {
        UserManager current = userManagerRef;
        if (current == null) {
            current = ComponentAccessor.getUserManager();
            userManagerRef = current;
        }
        return current;
    }

    private ProjectManager projectManager() {
        ProjectManager current = projectManagerRef;
        if (current == null) {
            current = ComponentAccessor.getProjectManager();
            projectManagerRef = current;
        }
        return current;
    }

    private UserSearchService userSearchService() {
        UserSearchService current = userSearchServiceRef;
        if (current == null) {
            current = ComponentAccessor.getComponent(UserSearchService.class);
            userSearchServiceRef = current;
        }
        return current;
    }

    private CrowdService crowdService() {
        CrowdService current = crowdServiceRef;
        if (current == null) {
            current = ComponentAccessor.getComponent(CrowdService.class);
            crowdServiceRef = current;
        }
        return current;
    }

    private CrowdDirectoryService crowdDirectoryService() {
        CrowdDirectoryService current = crowdDirectoryServiceRef;
        if (current == null) {
            current = ComponentAccessor.getComponent(CrowdDirectoryService.class);
            crowdDirectoryServiceRef = current;
        }
        return current;
    }

    private String jiraBaseUrl() {
        String base = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        if (base == null) {
            return "";
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private GroupManager groupManager() {
        GroupManager current = groupManagerRef;
        if (current == null) {
            current = ComponentAccessor.getGroupManager();
            groupManagerRef = current;
        }
        return current;
    }

    @Override
    public MetaDto meta(ApplicationUser user) {
        requireUser(user);
        I18nHelper i18n = authenticationContext().getI18nHelper();
        MetaDto meta = new MetaDto();
        meta.setVersion(PluginInfo.VERSION);
        meta.setBaseUrl(jiraBaseUrl());
        meta.setLocale(i18n.getLocale() == null ? "en" : i18n.getLocale().toLanguageTag());
        meta.setDisplayName(user.getDisplayName());
        meta.setUserKey(user.getKey());
        Map<String, String> labels = new LinkedHashMap<String, String>();
        for (String key : UiCatalog.KEYS) {
            labels.put(key, i18n.getText("asset-tree.ui." + key));
        }
        meta.setI18n(labels);
        List<ProjectDto> projects = visibleProjects(user);
        boolean editable = false;
        boolean configurable = false;
        boolean grantable = false;
        for (ProjectDto project : projects) {
            if (project.isCanEdit() || project.isCanCreate() || project.isCanMove()
                    || project.isCanRemove() || project.isCanComment()) {
                editable = true;
            }
            if (project.isCanConfigure() || project.isCanGrant()) {
                configurable = true;
            }
            if (project.isCanGrant()) {
                grantable = true;
            }
        }
        meta.setCanEdit(editable);
        meta.setCanConfigure(configurable);
        meta.setCanGrant(grantable);
        attachCards(projects);
        attachAssetKeys(projects);
        attachViews(projects);
        meta.setProjects(projects);
        return meta;
    }

    @Override
    public AssetListDto listAssets(ApplicationUser user, String projectKey, String query) {
        Project project = requireReadableProject(user, projectKey);
        final String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        final String key = project.getKey();
        final List<ServiceNotice> notices = new ArrayList<ServiceNotice>();
        AssetListDto list = ao().executeInTransaction(new TransactionCallback<AssetListDto>() {
            @Override
            public AssetListDto doInTransaction() {
                AssetEntity[] rows = assetsIn(key);
                applyDueFor(key, rows, notices);
                Map<Integer, List<AssetAttributeEntity>> attributes = attributesByAsset(idsOf(rows));
                Map<String, String[]> holders = new HashMap<String, String[]>();
                List<AssetEntity> matched = new ArrayList<AssetEntity>();
                for (AssetEntity row : rows) {
                    if (!needle.isEmpty() && !matches(row, attributes.get(row.getID()), needle, holders)) {
                        continue;
                    }
                    matched.add(row);
                    if (!needle.isEmpty() && matched.size() >= SEARCH_LIMIT) {
                        break;
                    }
                }
                Collections.sort(matched, SIBLING_ORDER);
                List<AssetTypeDto> types = typeDtos(key, rows);
                Map<String, AssetTypeDto> typeIndex = indexTypes(types);
                Map<Integer, AssetEntity> index = indexAssets(rows);
                AssetListDto list = new AssetListDto();
                List<AssetDto> assets = new ArrayList<AssetDto>();
                for (AssetEntity row : matched) {
                    assets.add(toDto(row, attributes.get(row.getID()), typeIndex, index, false));
                }
                list.setAssets(assets);
                list.setTypes(types);
                list.setStatuses(statusDtos(key, rows));
                return list;
            }
        });
        dispatchNotices(notices);
        return list;
    }

    @Override
    public AssetDto getAsset(ApplicationUser user, int id) {
        requireUser(user);
        final List<ServiceNotice> notices = new ArrayList<ServiceNotice>();
        AssetDto dto = ao().executeInTransaction(new TransactionCallback<AssetDto>() {
            @Override
            public AssetDto doInTransaction() {
                AssetEntity entity = requireReadable(user, id);
                String projectKey = entity.getProjectKey();
                AssetEntity[] rows = assetsIn(projectKey);
                applyDueFor(projectKey, rows, notices);
                entity = requireReadable(user, id);
                return toDto(entity, attributesFor(id), indexTypes(typeDtos(projectKey, rows)), indexAssets(rows), true);
            }
        });
        dispatchNotices(notices);
        return dto;
    }

    @Override
    public AssetDto createAsset(ApplicationUser user, AssetDraft draft) {
        final String error = AssetValidator.validateDraft(draft);
        if (error != null) {
            throw new AssetException(400, error);
        }
        AssetTypeEntity previewType = findType(draft.getTypeKey());
        String createCap = previewType != null && previewType.isLocation() ? GrantCaps.PLACES : GrantCaps.OBJECT;
        Project project = requireProjectCap(user, draft.getProjectKey(), createCap);
        if (storedAssetPrefix(project.getKey()).isEmpty()) {
            throw new AssetException(400, "asset-tree.error.assetKey.required");
        }
        return ao().executeInTransaction(new TransactionCallback<AssetDto>() {
            @Override
            public AssetDto doInTransaction() {
                String typeKey = draft.getTypeKey() == null ? "" : draft.getTypeKey().trim();
                AssetTypeEntity type = requireType(typeKey);
                if (!ProjectKeys.same(type.getProjectKey(), project.getKey())) {
                    throw new AssetException(400, "asset-tree.error.type");
                }
                Integer parentId = TreeLogic.normalizeParent(draft.getParentId());
                if (parentId != null) {
                    AssetEntity parent = requireReadable(user, parentId.intValue());
                    if (!ProjectKeys.same(parent.getProjectKey(), project.getKey())) {
                        throw new AssetException(400, "asset-tree.error.project.mismatch");
                    }
                }
                Map<Integer, Integer> parents = parentMap(assetsIn(project.getKey()));
                if (parentId != null && !parents.containsKey(parentId)) {
                    throw new AssetException(400, "asset-tree.error.parent");
                }
                if (parentId != null && TreeLogic.depth(parents, parentId.intValue()) + 1 >= TreeLogic.MAX_DEPTH) {
                    throw new AssetException(400, "asset-tree.error.depth");
                }
                String custodian = normalizeCustodian(draft.getCustodianKey());
                Date now = new Date();
                AssetEntity entity = ao().create(AssetEntity.class,
                        new DBParam("NAME", draft.getName().trim()),
                        new DBParam("OBJECT_KEY", nextKey(project.getKey())),
                        new DBParam("DESCRIPTION", draft.getDescription() == null ? "" : draft.getDescription()),
                        new DBParam("TYPE_KEY", type.getTypeKey()),
                        new DBParam("STATUS", resolveStatus(project.getKey(), draft.getStatus())),
                        new DBParam("SORT_ORDER", nextSort(parentId, project.getKey())),
                        new DBParam("CREATED", now),
                        new DBParam("UPDATED", now),
                        new DBParam("CREATED_BY", user.getKey()),
                        new DBParam("UPDATED_BY", user.getKey()),
                        new DBParam("PROJECT_KEY", project.getKey()));
                if (parentId != null) {
                    entity.setParentId(parentId);
                }
                entity.setCustodianKey(custodian);
                entity.save();
                replaceAttributes(entity, draft.getAttributes());
                AssetEntity[] rows = assetsIn(project.getKey());
                logActivity(entity.getID(), user.getKey(), "created", "", "", entity.getName());
                logPlaceChange(user, placeContainer(parentId, rows, null), "place_add", entity);
                return toDto(entity, attributesFor(entity.getID()), indexTypes(typeDtos(project.getKey(), rows)), indexAssets(rows), true);
            }
        });
    }

    @Override
    public AssetDto copyAsset(final ApplicationUser user, final int id) {
        AssetDraft draft = ao().executeInTransaction(new TransactionCallback<AssetDraft>() {
            @Override
            public AssetDraft doInTransaction() {
                AssetEntity source = requireReadable(user, id);
                AssetTypeEntity type = findType(source.getTypeKey());
                if (type == null || type.isLocation()) {
                    throw new AssetException(400, "asset-tree.error.copy");
                }
                requireProjectCap(user, source.getProjectKey(), GrantCaps.OBJECT);
                Set<String> taken = new HashSet<String>();
                for (AssetEntity row : assetsIn(source.getProjectKey())) {
                    if (row.getName() != null) {
                        taken.add(row.getName());
                    }
                }
                AssetDraft copy = new AssetDraft();
                copy.setName(CopyNames.next(source.getName(), i18n().getText("asset-tree.ui.copyWord"), taken));
                copy.setDescription(source.getDescription() == null ? "" : source.getDescription());
                copy.setTypeKey(source.getTypeKey());
                copy.setStatus(source.getStatus());
                copy.setParentId(source.getParentId());
                copy.setProjectKey(source.getProjectKey());
                copy.setCustodianKey(source.getCustodianKey());
                List<AttributeDraft> attributes = new ArrayList<AttributeDraft>();
                for (Map.Entry<String, String> entry : attributeValues(id).entrySet()) {
                    AttributeDraft item = new AttributeDraft();
                    item.setFieldKey(entry.getKey());
                    item.setValue(entry.getValue());
                    attributes.add(item);
                }
                copy.setAttributes(attributes);
                return copy;
            }
        });
        return createAsset(user, draft);
    }

    @Override
    public AssetDto updateAsset(ApplicationUser user, int id, AssetDraft draft) {
        final String error = AssetValidator.validateDraft(draft);
        if (error != null) {
            throw new AssetException(400, error);
        }
        return ao().executeInTransaction(new TransactionCallback<AssetDto>() {
            @Override
            public AssetDto doInTransaction() {
                AssetEntity entity = requireWritable(user, id);
                String typeKey = draft.getTypeKey() == null || draft.getTypeKey().trim().isEmpty()
                        ? entity.getTypeKey() : draft.getTypeKey().trim();
                AssetTypeEntity type = requireType(typeKey);
                if (!ProjectKeys.same(type.getProjectKey(), entity.getProjectKey())) {
                    throw new AssetException(400, "asset-tree.error.type");
                }
                String oldName = entity.getName() == null ? "" : entity.getName();
                String oldDescription = entity.getDescription() == null ? "" : entity.getDescription();
                String oldTypeKey = entity.getTypeKey() == null ? "" : entity.getTypeKey();
                String oldStatus = Statuses.canonical(entity.getStatus());
                String oldCustodian = entity.getCustodianKey() == null ? "" : entity.getCustodianKey();
                Map<String, String> oldAttributes = attributeValues(id);
                String newName = draft.getName().trim();
                String newDescription = draft.getDescription() == null ? "" : draft.getDescription();
                String newStatus = resolveStatus(entity.getProjectKey(), draft.getStatus() == null ? entity.getStatus() : draft.getStatus());
                String newCustodian = normalizeCustodian(draft.getCustodianKey());
                String storedCustodian = newCustodian == null ? "" : newCustodian;
                entity.setName(newName);
                entity.setDescription(newDescription);
                entity.setTypeKey(type.getTypeKey());
                entity.setStatus(newStatus);
                entity.setCustodianKey(newCustodian);
                entity.setUpdated(new Date());
                entity.setUpdatedBy(user.getKey());
                entity.save();
                if (draft.getAttributes() != null) {
                    replaceAttributes(entity, draft.getAttributes());
                }
                logActivity(id, user.getKey(), "name", "", oldName, newName);
                logActivity(id, user.getKey(), "description", "", clip(oldDescription), clip(newDescription));
                logActivity(id, user.getKey(), "type", "", typeLabel(requireType(oldTypeKey)), typeLabel(type));
                logActivity(id, user.getKey(), "status", "", oldStatus, newStatus);
                logActivity(id, user.getKey(), "custodian", "", displayName(oldCustodian), displayName(storedCustodian));
                if (draft.getAttributes() != null) {
                    logAttributeChanges(id, user.getKey(), entity.getTypeKey(), oldAttributes);
                }
                AssetEntity[] rows = assetsIn(entity.getProjectKey());
                return toDto(entity, attributesFor(id), indexTypes(typeDtos(entity.getProjectKey(), rows)), indexAssets(rows), true);
            }
        });
    }

    @Override
    public CommentDto addComment(ApplicationUser user, int id, String body) {
        requireAssetCap(user, id, kindCap(requireAsset(id)));
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            throw new AssetException(400, "asset-tree.error.comment.required");
        }
        if (text.length() > AssetValidator.MAX_DESCRIPTION) {
            throw new AssetException(400, "asset-tree.error.description.length");
        }
        final String saved = text;
        AssetCommentEntity created = ao().executeInTransaction(new TransactionCallback<AssetCommentEntity>() {
            @Override
            public AssetCommentEntity doInTransaction() {
                return ao().create(AssetCommentEntity.class,
                        new DBParam("ASSET_ID", id),
                        new DBParam("AUTHOR_KEY", user.getKey()),
                        new DBParam("BODY", saved),
                        new DBParam("CREATED", new Date()));
            }
        });
        logActivity(id, user.getKey(), "comment", "", "", clip(saved));
        return commentDto(created);
    }

    @Override
    public void deleteComment(ApplicationUser user, int assetId, int commentId) {
        AssetEntity asset = requireAsset(assetId);
        AssetCommentEntity comment = ao().get(AssetCommentEntity.class, commentId);
        if (comment == null || comment.getAssetId() != assetId) {
            requireAssetCap(user, assetId, kindCap(asset));
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        boolean admin = canArrangeTypes(user, project(asset.getProjectKey()));
        if (!admin) {
            requireAssetCap(user, assetId, kindCap(asset));
            if (comment.getAuthorKey() == null || !comment.getAuthorKey().equals(user.getKey())) {
                throw new AssetException(403, "asset-tree.error.forbidden");
            }
        }
        logActivity(assetId, user.getKey(), "comment_delete", "", clip(comment.getBody()), "");
        ao().delete(comment);
    }

    @Override
    public void deleteActivity(ApplicationUser user, int assetId, int activityId) {
        AssetEntity asset = requireAsset(assetId);
        Project project = project(asset.getProjectKey());
        if (!canArrangeTypes(user, project)) {
            requireAssetCap(user, assetId, kindCap(asset));
            throw new AssetException(403, "asset-tree.error.forbidden");
        }
        AssetActivityEntity row = ao().get(AssetActivityEntity.class, activityId);
        if (row == null || row.getAssetId() != assetId) {
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        ao().delete(row);
    }

    @Override
    public FileDto storeFile(ApplicationUser user, int assetId, String fileName, String contentType, byte[] data) {
        requireAssetCap(user, assetId, kindCap(requireAsset(assetId)));
        if (data == null || data.length == 0) {
            throw new AssetException(400, "asset-tree.error.file.required");
        }
        if (data.length > AssetFileStore.MAX_BYTES) {
            throw new AssetException(400, "asset-tree.error.file.size");
        }
        final String safeName = fileNameOf(fileName);
        final String safeType = contentType == null || contentType.trim().isEmpty() ? "application/octet-stream" : contentType.trim();
        final byte[] bytes = data;
        AssetFileEntity created = ao().executeInTransaction(new TransactionCallback<AssetFileEntity>() {
            @Override
            public AssetFileEntity doInTransaction() {
                return ao().create(AssetFileEntity.class,
                        new DBParam("ASSET_ID", assetId),
                        new DBParam("FILE_NAME", safeName),
                        new DBParam("CONTENT_TYPE", safeType),
                        new DBParam("SIZE_BYTES", Long.valueOf(bytes.length)),
                        new DBParam("AUTHOR_KEY", user.getKey()),
                        new DBParam("CREATED", new Date()));
            }
        });
        try {
            AssetFileStore.write(created.getID(), bytes);
        } catch (IOException ex) {
            ao().delete(created);
            throw new AssetException(500, "asset-tree.error.unexpected");
        }
        logActivity(assetId, user.getKey(), "file", "", "", safeName);
        return fileDto(created);
    }

    @Override
    public StoredFile openFile(ApplicationUser user, int fileId) {
        AssetFileEntity file = ao().get(AssetFileEntity.class, fileId);
        if (file == null) {
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        requireReadable(user, file.getAssetId());
        java.io.File stored = AssetFileStore.file(file.getID());
        if (!stored.isFile()) {
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        byte[] data = new byte[(int) stored.length()];
        try {
            FileInputStream input = new FileInputStream(stored);
            try {
                int offset = 0;
                while (offset < data.length) {
                    int read = input.read(data, offset, data.length - offset);
                    if (read < 0) {
                        break;
                    }
                    offset += read;
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            throw new AssetException(500, "asset-tree.error.unexpected");
        }
        return new StoredFile(file.getFileName(), file.getContentType(), data);
    }

    @Override
    public void deleteFile(ApplicationUser user, int assetId, int fileId) {
        requireAssetCap(user, assetId, kindCap(requireAsset(assetId)));
        AssetFileEntity file = ao().get(AssetFileEntity.class, fileId);
        if (file == null || file.getAssetId() != assetId) {
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        logActivity(assetId, user.getKey(), "file_delete", "", file.getFileName(), "");
        ao().delete(file);
        AssetFileStore.delete(fileId);
    }

    @Override
    public void storeCardImage(ApplicationUser user, int assetId, byte[] data) {
        if (data == null || data.length == 0) {
            throw new AssetException(400, "asset-tree.error.file.required");
        }
        if (data.length > CardImageStore.MAX_BYTES) {
            throw new AssetException(400, "asset-tree.error.cardImage.size");
        }
        final String kind = CardImageStore.kindOf(data);
        if (kind.isEmpty()) {
            throw new AssetException(400, "asset-tree.error.cardImage.type");
        }
        final byte[] bytes = data;
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                AssetEntity entity = requireAssetCap(user, assetId, kindCap(requireAsset(assetId)));
                if (isLocation(entity)) {
                    throw new AssetException(400, "asset-tree.error.cardImage.place");
                }
                entity.setCardImage(kind);
                entity.setUpdated(new Date());
                entity.setUpdatedBy(user.getKey());
                entity.save();
                return null;
            }
        });
        try {
            CardImageStore.write(assetId, bytes);
        } catch (IOException ex) {
            clearCardImage(assetId);
            throw new AssetException(500, "asset-tree.error.unexpected");
        }
        logActivity(assetId, user.getKey(), "card_image", "", "", "1");
    }

    @Override
    public StoredFile openCardImage(ApplicationUser user, int assetId) {
        AssetEntity entity = requireReadable(user, assetId);
        String kind = entity.getCardImage();
        if (kind == null || kind.isEmpty()) {
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        java.io.File stored = CardImageStore.file(assetId);
        if (!stored.isFile()) {
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        byte[] data = new byte[(int) stored.length()];
        try {
            FileInputStream input = new FileInputStream(stored);
            try {
                int offset = 0;
                while (offset < data.length) {
                    int read = input.read(data, offset, data.length - offset);
                    if (read < 0) {
                        break;
                    }
                    offset += read;
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            throw new AssetException(500, "asset-tree.error.unexpected");
        }
        String name = "jpeg".equals(kind) ? "card.jpg" : "card.png";
        return new StoredFile(name, CardImageStore.contentType(kind), data);
    }

    @Override
    public void deleteCardImage(ApplicationUser user, int assetId) {
        final boolean[] had = new boolean[] {false};
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                AssetEntity entity = requireAssetCap(user, assetId, kindCap(requireAsset(assetId)));
                String kind = entity.getCardImage();
                had[0] = kind != null && !kind.isEmpty();
                entity.setCardImage("");
                entity.setUpdated(new Date());
                entity.setUpdatedBy(user.getKey());
                entity.save();
                return null;
            }
        });
        CardImageStore.delete(assetId);
        if (had[0]) {
            logActivity(assetId, user.getKey(), "card_image_delete", "", "1", "");
        }
    }

    private void clearCardImage(final int assetId) {
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                AssetEntity entity = ao().get(AssetEntity.class, assetId);
                if (entity != null) {
                    entity.setCardImage("");
                    entity.save();
                }
                return null;
            }
        });
    }

    @Override
    public void deleteAsset(ApplicationUser user, int id, boolean cascade) {
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                AssetEntity entity = requireAssetCap(user, id, kindCap(requireAsset(id)));
                AssetEntity[] all = assetsIn(entity.getProjectKey());
                List<Integer> descendants = TreeLogic.descendants(childrenMap(all), id);
                if (!descendants.isEmpty() && !cascade) {
                    throw new AssetException(409, "asset-tree.error.hasChildren", Integer.valueOf(descendants.size()));
                }
                List<Integer> toDelete = new ArrayList<Integer>(descendants);
                toDelete.add(Integer.valueOf(id));
                Map<Integer, AssetEntity> index = indexAssets(all);
                Set<Integer> doomed = new HashSet<Integer>(toDelete);
                for (Integer assetId : toDelete) {
                    AssetEntity gone = index.get(assetId);
                    if (gone == null) {
                        continue;
                    }
                    logPlaceChange(user, placeContainer(TreeLogic.normalizeParent(gone.getParentId()), all, doomed), "place_remove", gone);
                }
                List<Integer> fileIds = new ArrayList<Integer>();
                for (AssetFileEntity file : findIn(AssetFileEntity.class, "ASSET_ID", toDelete, null)) {
                    fileIds.add(Integer.valueOf(file.getID()));
                }
                deleteRows(AssetAttributeEntity.class, "ASSET_ID", toDelete);
                deleteRows(AssetIssueLinkEntity.class, "ASSET_ID", toDelete);
                deleteRows(InventoryMarkEntity.class, "ASSET_ID", toDelete);
                deleteRows(AssetCommentEntity.class, "ASSET_ID", toDelete);
                deleteRows(AssetFileEntity.class, "ASSET_ID", toDelete);
                deleteRows(AssetActivityEntity.class, "ASSET_ID", toDelete);
                deleteRows(ServicePlanEntity.class, "ASSET_ID", toDelete);
                deleteRows(LifecycleEntity.class, "ASSET_ID", toDelete);
                deleteRows(PinEntity.class, "ASSET_ID", toDelete);
                List<Integer> ruleIds = new ArrayList<Integer>();
                for (PortalRuleEntity rule : findIn(PortalRuleEntity.class, "ASSET_ID", toDelete, null)) {
                    ruleIds.add(Integer.valueOf(rule.getID()));
                }
                deleteRows(PortalRuleConditionEntity.class, "RULE_ID", ruleIds);
                deleteRows(PortalRuleEntity.class, "ID", ruleIds);
                for (Integer fileId : fileIds) {
                    AssetFileStore.delete(fileId.intValue());
                }
                for (Integer assetId : toDelete) {
                    CardImageStore.delete(assetId.intValue());
                }
                for (int i = descendants.size() - 1; i >= 0; i--) {
                    AssetEntity child = ao().get(AssetEntity.class, descendants.get(i));
                    if (child != null) {
                        ao().delete(child);
                    }
                }
                ao().delete(entity);
                return null;
            }
        });
    }

    @Override
    public AssetDto moveAsset(ApplicationUser user, int id, MoveDraft move) {
        if (move == null) {
            throw new AssetException(400, "asset-tree.error.parent");
        }
        return ao().executeInTransaction(new TransactionCallback<AssetDto>() {
            @Override
            public AssetDto doInTransaction() {
                AssetEntity moving = requireAssetCap(user, id, kindCap(requireAsset(id)));
                Integer parentId = TreeLogic.normalizeParent(move.getParentId());
                if (parentId != null) {
                    AssetEntity parent = requireReadable(user, parentId.intValue());
                    if (!ProjectKeys.same(parent.getProjectKey(), moving.getProjectKey())) {
                        throw new AssetException(400, "asset-tree.error.project.mismatch");
                    }
                }
                AssetEntity[] all = assetsIn(moving.getProjectKey());
                Map<Integer, Integer> parents = parentMap(all);
                if (parentId != null && !parents.containsKey(parentId)) {
                    throw new AssetException(400, "asset-tree.error.parent");
                }
                if (TreeLogic.wouldCycle(parents, id, parentId)) {
                    throw new AssetException(400, "asset-tree.error.cycle");
                }
                int baseDepth = parentId == null ? 0 : TreeLogic.depth(parents, parentId.intValue()) + 1;
                int extra = TreeLogic.subtreeHeight(childrenMap(all), id);
                if (baseDepth >= TreeLogic.MAX_DEPTH || baseDepth + extra >= TreeLogic.MAX_DEPTH) {
                    throw new AssetException(400, "asset-tree.error.depth");
                }
                List<AssetEntity> siblings = new ArrayList<AssetEntity>();
                for (AssetEntity candidate : all) {
                    if (candidate.getID() == id) {
                        continue;
                    }
                    if (TreeLogic.sameParent(candidate.getParentId(), parentId)) {
                        siblings.add(candidate);
                    }
                }
                Collections.sort(siblings, SIBLING_ORDER);
                Integer previousParent = TreeLogic.normalizeParent(moving.getParentId());
                Integer fromPlace = placeContainer(previousParent, all, null);
                Integer toPlace = placeContainer(parentId, all, null);
                int index = move.getIndex() == null ? siblings.size() : move.getIndex().intValue();
                if (index < 0) {
                    index = 0;
                }
                if (index > siblings.size()) {
                    index = siblings.size();
                }
                moving.setParentId(parentId);
                moving.setUpdated(new Date());
                moving.setUpdatedBy(user.getKey());
                if (!TreeLogic.sameParent(previousParent, parentId)) {
                    logActivity(id, user.getKey(), "move", "", parentName(previousParent), parentName(parentId));
                }
                if (fromPlace != null && !fromPlace.equals(toPlace)) {
                    logPlaceChange(user, fromPlace, "place_out", moving);
                }
                if (toPlace != null && !toPlace.equals(fromPlace)) {
                    logPlaceChange(user, toPlace, "place_in", moving);
                }
                siblings.add(index, moving);
                for (int i = 0; i < siblings.size(); i++) {
                    AssetEntity sibling = siblings.get(i);
                    sibling.setSortOrder(i);
                    sibling.save();
                }
                return toDto(moving, attributesFor(id), indexTypes(typeDtos(moving.getProjectKey(), all)), indexAssets(all), true);
            }
        });
    }

    @Override
    public AssetTypeDto createType(ApplicationUser user, TypeDraft draft) {
        if (draft == null) {
            throw new AssetException(400, "asset-tree.error.type.label");
        }
        Project project = requireConfigurableProject(user, draft.getProjectKey());
        String labelError = AssetValidator.validateTypeLabel(draft.getLabel());
        if (labelError != null) {
            throw new AssetException(400, labelError);
        }
        String color = draft.getColor();
        if (color == null || color.trim().isEmpty()) {
            color = DefaultTypes.PALETTE[Math.floorMod(draft.getLabel().hashCode(), DefaultTypes.PALETTE.length)];
        }
        String colorError = AssetValidator.validateColor(color);
        if (colorError != null) {
            throw new AssetException(400, colorError);
        }
        final String chosenColor = color;
        final String chosenIcon = resolveIcon(draft.getIcon(), draft.isLocation());
        String caption = "";
        if (draft.isLocation() && draft.getPlaceCaption() != null) {
            String captionError = AssetValidator.validateCaption(draft.getPlaceCaption());
            if (captionError != null) {
                throw new AssetException(400, captionError);
            }
            caption = draft.getPlaceCaption().trim();
        }
        final String chosenCaption = caption;
        return ao().executeInTransaction(new TransactionCallback<AssetTypeDto>() {
            @Override
            public AssetTypeDto doInTransaction() {
                AssetTypeEntity[] existing = ao().find(AssetTypeEntity.class);
                Set<String> taken = new HashSet<String>();
                int maxOrder = -1;
                for (AssetTypeEntity type : existing) {
                    taken.add(type.getTypeKey());
                    if (ProjectKeys.same(type.getProjectKey(), project.getKey()) && type.getSortOrder() > maxOrder) {
                        maxOrder = type.getSortOrder();
                    }
                }
                String key = TreeLogic.uniqueKey(TreeLogic.slug(draft.getLabel()), taken);
                if (!AssetValidator.isTypeKey(key)) {
                    throw new AssetException(400, "asset-tree.error.type.duplicate");
                }
                String parentKey = parentTypeKey(project.getKey(), null, draft.getParentTypeKey(), draft.isLocation());
                AssetTypeEntity created = ao().create(AssetTypeEntity.class,
                        new DBParam("TYPE_KEY", key),
                        new DBParam("LABEL", draft.getLabel().trim()),
                        new DBParam("COLOR", chosenColor),
                        new DBParam("ICON", chosenIcon),
                        new DBParam("SYSTEM_TYPE", Boolean.FALSE),
                        new DBParam("SORT_ORDER", maxOrder + 1),
                        new DBParam("PROJECT_KEY", project.getKey()),
                        new DBParam("BASE_KEY", ""),
                        new DBParam("LOCATION", Boolean.valueOf(draft.isLocation())),
                        new DBParam("SHOW_IN_TREE", Boolean.valueOf(draft.isLocation() || Boolean.TRUE.equals(draft.getShowInTree()))),
                        new DBParam("SERVICE", Boolean.valueOf(!draft.isLocation() && Boolean.TRUE.equals(draft.getService()))),
                        new DBParam("PLACE_CAPTION", chosenCaption),
                        new DBParam("PARENT_TYPE_KEY", parentKey));
                return toTypeDto(created, i18n(), 0);
            }
        });
    }

    @Override
    public AssetTypeDto updateType(ApplicationUser user, String typeKey, TypeDraft draft) {
        if (draft == null) {
            throw new AssetException(400, "asset-tree.error.type.notFound");
        }
        return ao().executeInTransaction(new TransactionCallback<AssetTypeDto>() {
            @Override
            public AssetTypeDto doInTransaction() {
                AssetTypeEntity type = findType(typeKey);
                if (type == null) {
                    throw new AssetException(404, "asset-tree.error.type.notFound");
                }
                requireConfigurableProject(user, type.getProjectKey());
                if (draft.getShowInTree() != null) {
                    type.setShowInTree(type.isLocation() || draft.getShowInTree().booleanValue());
                }
                if (draft.getService() != null && !type.isLocation()) {
                    type.setService(draft.getService().booleanValue());
                }
                if (draft.getIcon() != null) {
                    type.setIcon(resolveIcon(draft.getIcon(), type.isLocation()));
                }
                if (draft.getColor() != null) {
                    String colorError = AssetValidator.validateColor(draft.getColor().trim());
                    if (colorError != null) {
                        throw new AssetException(400, colorError);
                    }
                    type.setColor(draft.getColor().trim());
                }
                if (draft.getPlaceCaption() != null && type.isLocation()) {
                    String captionError = AssetValidator.validateCaption(draft.getPlaceCaption());
                    if (captionError != null) {
                        throw new AssetException(400, captionError);
                    }
                    type.setPlaceCaption(draft.getPlaceCaption().trim());
                }
                if (draft.getParentTypeKey() != null) {
                    type.setParentTypeKey(parentTypeKey(type.getProjectKey(), type.getTypeKey(),
                            draft.getParentTypeKey(), type.isLocation()));
                }
                type.save();
                int count = ao().count(AssetEntity.class, "TYPE_KEY = ?", type.getTypeKey());
                return toTypeDto(type, i18n(), count);
            }
        });
    }

    @Override
    public void deleteType(ApplicationUser user, String typeKey) {
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                AssetTypeEntity type = findType(typeKey);
                if (type == null) {
                    throw new AssetException(404, "asset-tree.error.type.notFound");
                }
                requireConfigurableProject(user, type.getProjectKey());
                if (type.isSystemType()) {
                    throw new AssetException(400, "asset-tree.error.type.system");
                }
                int used = ao().count(AssetEntity.class, "TYPE_KEY = ?", type.getTypeKey());
                if (used > 0) {
                    throw new AssetException(409, "asset-tree.error.type.inUse");
                }
                if (hasChildTypes(type.getTypeKey())) {
                    throw new AssetException(409, "asset-tree.error.type.hasSubtypes");
                }
                ao().deleteWithSQL(AssetFieldEntity.class, "TYPE_KEY = ?", type.getTypeKey());
                ao().delete(type);
                return null;
            }
        });
    }

    @Override
    public FieldDto addField(ApplicationUser user, String typeKey, FieldDraft draft) {
        if (draft == null) {
            throw new AssetException(400, "asset-tree.error.field.label");
        }
        String labelError = AssetValidator.validateTypeLabel(draft.getLabel());
        if (labelError != null) {
            throw new AssetException(400, "asset-tree.error.field.label");
        }
        if (!FieldKinds.isKind(draft.getKind())) {
            throw new AssetException(400, "asset-tree.error.field.kind");
        }
        final String storedOptions;
        if (FieldChoices.needsOptions(draft.getKind())) {
            storedOptions = FieldChoices.canonicalOptions(draft.getOptions());
            if (storedOptions == null) {
                throw new AssetException(400, "asset-tree.error.field.options");
            }
        } else {
            storedOptions = "";
        }
        return ao().executeInTransaction(new TransactionCallback<FieldDto>() {
            @Override
            public FieldDto doInTransaction() {
                AssetTypeEntity type = findType(typeKey);
                if (type == null) {
                    throw new AssetException(404, "asset-tree.error.type.notFound");
                }
                requireConfigurableProject(user, type.getProjectKey());
                AssetFieldEntity[] existing = fields(type.getTypeKey());
                Set<String> taken = new HashSet<String>();
                int max = -1;
                for (AssetFieldEntity field : existing) {
                    taken.add(field.getFieldKey());
                    if (field.getPosition() > max) {
                        max = field.getPosition();
                    }
                }
                String fieldKey = TreeLogic.uniqueKey(TreeLogic.slug(draft.getLabel()), taken);
                if (!AssetValidator.isTypeKey(fieldKey)) {
                    throw new AssetException(400, "asset-tree.error.field.label");
                }
                AssetFieldEntity created = ao().create(AssetFieldEntity.class,
                        new DBParam("TYPE_KEY", type.getTypeKey()),
                        new DBParam("FIELD_KEY", fieldKey),
                        new DBParam("SCOPE_KEY", type.getTypeKey() + ":" + fieldKey),
                        new DBParam("LABEL", draft.getLabel().trim()),
                        new DBParam("KIND", draft.getKind()),
                        new DBParam("REQUIRED", Boolean.valueOf(draft.isRequired())),
                        new DBParam("ISSUE_HIDDEN", Boolean.valueOf(hidesOnIssue(draft))),
                        new DBParam("POSITION", max + 1),
                        new DBParam("OPTIONS", storedOptions));
                return toFieldDto(created, i18n());
            }
        });
    }

    @Override
    public FieldDto updateField(ApplicationUser user, final String typeKey, final String fieldKey, final FieldDraft draft) {
        if (draft == null || (draft.getShowOnIssue() == null && draft.getLabel() == null && draft.getKind() == null
                && draft.getRequired() == null && draft.getOptions() == null)) {
            throw new AssetException(400, "asset-tree.error.field.label");
        }
        if (draft.getLabel() != null && AssetValidator.validateTypeLabel(draft.getLabel()) != null) {
            throw new AssetException(400, "asset-tree.error.field.label");
        }
        if (draft.getKind() != null && !FieldKinds.isKind(draft.getKind())) {
            throw new AssetException(400, "asset-tree.error.field.kind");
        }
        return ao().executeInTransaction(new TransactionCallback<FieldDto>() {
            @Override
            public FieldDto doInTransaction() {
                AssetTypeEntity type = findType(typeKey);
                if (type == null) {
                    throw new AssetException(404, "asset-tree.error.type.notFound");
                }
                requireConfigurableProject(user, type.getProjectKey());
                AssetFieldEntity field = findField(type.getTypeKey(), fieldKey);
                if (field == null) {
                    throw new AssetException(404, "asset-tree.error.field.notFound");
                }
                if (draft.getLabel() != null) {
                    field.setLabel(draft.getLabel().trim());
                }
                String kind = draft.getKind() != null ? draft.getKind() : field.getKind();
                if (draft.getKind() != null || draft.getOptions() != null) {
                    String storedOptions = "";
                    if (FieldChoices.needsOptions(kind)) {
                        String raw = draft.getOptions() != null ? draft.getOptions() : field.getOptions();
                        storedOptions = FieldChoices.canonicalOptions(raw);
                        if (storedOptions == null) {
                            throw new AssetException(400, "asset-tree.error.field.options");
                        }
                    }
                    if (draft.getKind() != null) {
                        field.setKind(draft.getKind());
                    }
                    field.setOptions(storedOptions);
                }
                if (draft.getShowOnIssue() != null) {
                    field.setIssueHidden(!draft.getShowOnIssue().booleanValue());
                }
                if (draft.getRequired() != null) {
                    field.setRequired(draft.getRequired().booleanValue());
                }
                field.save();
                return toFieldDto(field, i18n());
            }
        });
    }

    @Override
    public List<FieldDto> reorderFields(ApplicationUser user, final String typeKey, final FieldOrderDto order) {
        if (order == null) {
            throw new AssetException(400, "asset-tree.error.field.notFound");
        }
        return ao().executeInTransaction(new TransactionCallback<List<FieldDto>>() {
            @Override
            public List<FieldDto> doInTransaction() {
                AssetTypeEntity type = findType(typeKey);
                if (type == null) {
                    throw new AssetException(404, "asset-tree.error.type.notFound");
                }
                requireConfigurableProject(user, type.getProjectKey());
                AssetFieldEntity[] existing = fields(type.getTypeKey());
                Map<String, AssetFieldEntity> byKey = new LinkedHashMap<String, AssetFieldEntity>();
                for (AssetFieldEntity field : existing) {
                    byKey.put(field.getFieldKey(), field);
                }
                List<AssetFieldEntity> ordered = new ArrayList<AssetFieldEntity>();
                for (String key : order.getFieldKeys()) {
                    AssetFieldEntity field = byKey.remove(key);
                    if (field != null) {
                        ordered.add(field);
                    }
                }
                ordered.addAll(byKey.values());
                I18nHelper i18n = i18n();
                List<FieldDto> result = new ArrayList<FieldDto>();
                for (int i = 0; i < ordered.size(); i++) {
                    AssetFieldEntity field = ordered.get(i);
                    if (field.getPosition() != i) {
                        field.setPosition(i);
                        field.save();
                    }
                    result.add(toFieldDto(field, i18n));
                }
                return result;
            }
        });
    }

    @Override
    public void deleteField(ApplicationUser user, String typeKey, String fieldKey) {
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                AssetTypeEntity type = findType(typeKey);
                if (type == null) {
                    throw new AssetException(404, "asset-tree.error.type.notFound");
                }
                requireConfigurableProject(user, type.getProjectKey());
                AssetFieldEntity field = findField(type.getTypeKey(), fieldKey);
                if (field == null) {
                    throw new AssetException(404, "asset-tree.error.field.notFound");
                }
                AssetEntity[] assets = ao().find(AssetEntity.class, Query.select().where("TYPE_KEY = ?", type.getTypeKey()));
                for (AssetEntity asset : assets) {
                    ao().deleteWithSQL(AssetAttributeEntity.class, "ASSET_ID = ? AND ATTR_NAME = ?", asset.getID(), field.getFieldKey());
                }
                ao().delete(field);
                return null;
            }
        });
    }

    @Override
    public List<StatusDto> listStatuses(ApplicationUser user, final String projectKey) {
        final Project project = requireReadableProject(user, projectKey);
        return ao().executeInTransaction(new TransactionCallback<List<StatusDto>>() {
            @Override
            public List<StatusDto> doInTransaction() {
                return statusDtos(project.getKey(), assetsIn(project.getKey()));
            }
        });
    }

    @Override
    public StatusDto createStatus(ApplicationUser user, String projectKey, final StatusDraft draft) {
        final Project project = requireConfigurableProject(user, projectKey);
        if (draft == null || draft.getLabel() == null || draft.getLabel().trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.status");
        }
        final String category = normalizeCategory(draft.getCategory());
        return ao().executeInTransaction(new TransactionCallback<StatusDto>() {
            @Override
            public StatusDto doInTransaction() {
                ensureStatuses(project.getKey());
                ProjectStatusEntity[] existing = projectStatuses(project.getKey());
                Set<String> taken = new HashSet<String>();
                int maxOrder = -1;
                for (ProjectStatusEntity row : existing) {
                    taken.add(row.getStatusKey());
                    if (row.getSortOrder() > maxOrder) {
                        maxOrder = row.getSortOrder();
                    }
                }
                String key = TreeLogic.uniqueKey(TreeLogic.slug(draft.getLabel()), taken);
                if (!AssetValidator.isStatusKey(key)) {
                    throw new AssetException(400, "asset-tree.error.status");
                }
                ProjectStatusEntity created = ao().create(ProjectStatusEntity.class,
                        new DBParam("PROJECT_KEY", project.getKey()),
                        new DBParam("STATUS_KEY", key),
                        new DBParam("LABEL", draft.getLabel().trim()),
                        new DBParam("CATEGORY", category),
                        new DBParam("SORT_ORDER", maxOrder + 1),
                        new DBParam("SUMMARY_MODE", StatusSummary.createMode(draft.getInSummary())));
                return toStatusDto(created, Integer.valueOf(0));
            }
        });
    }

    @Override
    public StatusDto updateStatus(ApplicationUser user, String projectKey, final String statusKey, final StatusDraft draft) {
        final Project project = requireConfigurableProject(user, projectKey);
        if (draft == null) {
            throw new AssetException(400, "asset-tree.error.status");
        }
        return ao().executeInTransaction(new TransactionCallback<StatusDto>() {
            @Override
            public StatusDto doInTransaction() {
                ensureStatuses(project.getKey());
                ProjectStatusEntity row = findStatus(project.getKey(), statusKey);
                if (row == null) {
                    throw new AssetException(404, "asset-tree.error.status");
                }
                if (draft.getLabel() != null) {
                    if (draft.getLabel().trim().isEmpty()) {
                        throw new AssetException(400, "asset-tree.error.status");
                    }
                    row.setLabel(draft.getLabel().trim());
                }
                if (draft.getCategory() != null && !draft.getCategory().trim().isEmpty()) {
                    row.setCategory(normalizeCategory(draft.getCategory()));
                }
                int summaryMode = StatusSummary.updateMode(draft.getInSummary(), row.getSummaryMode());
                if (summaryMode != row.getSummaryMode()) {
                    row.setSummaryMode(summaryMode);
                }
                row.save();
                return toStatusDto(row, Integer.valueOf(countStatus(project.getKey(), row.getStatusKey())));
            }
        });
    }

    @Override
    public void deleteStatus(ApplicationUser user, String projectKey, final String statusKey) {
        final Project project = requireConfigurableProject(user, projectKey);
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                ensureStatuses(project.getKey());
                ProjectStatusEntity[] rows = projectStatuses(project.getKey());
                if (rows.length <= 1) {
                    throw new AssetException(400, "asset-tree.error.status.last");
                }
                ProjectStatusEntity row = findStatus(project.getKey(), statusKey);
                if (row == null) {
                    throw new AssetException(404, "asset-tree.error.status");
                }
                if (countStatus(project.getKey(), row.getStatusKey()) > 0) {
                    throw new AssetException(409, "asset-tree.error.status.inUse");
                }
                String fallback = null;
                for (ProjectStatusEntity candidate : rows) {
                    if (!row.getStatusKey().equals(candidate.getStatusKey())) {
                        fallback = candidate.getStatusKey();
                        break;
                    }
                }
                if (fallback != null) {
                    releasePlaceStatus(project.getKey(), row.getStatusKey(), fallback);
                }
                ao().delete(row);
                return null;
            }
        });
    }

    @Override
    public List<GrantDto> listGrants(ApplicationUser user, final String projectKey) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.ACCESS);
        return ao().executeInTransaction(new TransactionCallback<List<GrantDto>>() {
            @Override
            public List<GrantDto> doInTransaction() {
                return grantDtos(project.getKey());
            }
        });
    }

    @Override
    public GrantDto addGrant(ApplicationUser user, String projectKey, final GrantDraft draft) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.ACCESS);
        if (draft == null || draft.getGroupName() == null || draft.getGroupName().trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.group");
        }
        final String groupName = draft.getGroupName().trim();
        final String caps = resolveCaps(draft);
        final String level = GrantCaps.levelOf(caps);
        if (!groupExists(groupName)) {
            throw new AssetException(400, "asset-tree.error.group");
        }
        final boolean administrator = GrantCaps.has(caps, GrantCaps.ADMIN);
        if (administrator && !hasCap(user, project, GrantCaps.ADMIN)) {
            throw new AssetException(403, "asset-tree.error.forbidden");
        }
        return ao().executeInTransaction(new TransactionCallback<GrantDto>() {
            @Override
            public GrantDto doInTransaction() {
                ProjectGrantEntity global = findGrant(GrantCaps.ALL_PROJECTS, groupName);
                if (global != null && GrantCaps.has(global.getCaps(), GrantCaps.ADMIN) && !hasCap(user, project, GrantCaps.ADMIN)) {
                    throw new AssetException(403, "asset-tree.error.forbidden");
                }
                String storedKey = administrator ? GrantCaps.ALL_PROJECTS : project.getKey();
                if (administrator) {
                    ProjectGrantEntity local = findGrant(project.getKey(), groupName);
                    if (local != null) {
                        ao().delete(local);
                    }
                } else if (global != null) {
                    ao().delete(global);
                    global = null;
                }
                ProjectGrantEntity row = findGrant(storedKey, groupName);
                if (row == null) {
                    row = ao().create(ProjectGrantEntity.class,
                            new DBParam("PROJECT_KEY", storedKey),
                            new DBParam("GROUP_NAME", groupName),
                            new DBParam("LEVEL", level),
                            new DBParam("CAPS", caps));
                } else {
                    row.setLevel(level);
                    row.setCaps(caps);
                    row.save();
                }
                return toGrantDto(row);
            }
        });
    }

    @Override
    public void deleteGrant(ApplicationUser user, String projectKey, final String groupName) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.ACCESS);
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.group");
        }
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                String name = groupName.trim();
                ProjectGrantEntity row = findGrant(project.getKey(), name);
                ProjectGrantEntity global = findGrant(GrantCaps.ALL_PROJECTS, name);
                if (global != null && !hasCap(user, project, GrantCaps.ADMIN)) {
                    throw new AssetException(403, "asset-tree.error.forbidden");
                }
                if (row == null && global == null) {
                    throw new AssetException(404, "asset-tree.error.group");
                }
                if (row != null) {
                    ao().delete(row);
                }
                if (global != null) {
                    ao().delete(global);
                }
                return null;
            }
        });
    }

    @Override
    public List<GrantDto> searchGroups(ApplicationUser user, String query) {
        requireUser(user);
        List<GrantDto> found = new ArrayList<GrantDto>();
        if (query == null || query.trim().isEmpty() || !canSearchGroups(user)) {
            return found;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        Collection<Group> groups = groupManager().getAllGroups();
        if (groups == null) {
            return found;
        }
        for (Group group : groups) {
            if (group == null || group.getName() == null) {
                continue;
            }
            if (!group.getName().toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            GrantDto dto = new GrantDto();
            dto.setGroupName(group.getName());
            found.add(dto);
            if (found.size() >= 15) {
                break;
            }
        }
        return found;
    }

    @Override
    public IssueRefDto linkIssue(ApplicationUser user, int assetId, String issueKey) {
        Issue issue = requireIssue(user, issueKey);
        IssueRefDto linked = link(user, assetId, issue, true);
        applyLinkedFlow(user, assetId, issue);
        return linked;
    }

    @Override
    public AssetDto linkAssetToIssue(ApplicationUser user, long issueId, int assetId) {
        Issue issue = issueManager().getIssueObject(Long.valueOf(issueId));
        if (!canBrowse(user, issue)) {
            throw new AssetException(404, "asset-tree.error.issue.notFound");
        }
        if (!canWorkIssue(user, issue)) {
            throw new AssetException(403, "asset-tree.error.forbidden");
        }
        link(user, assetId, issue, false);
        applyLinkedFlow(user, assetId, issue);
        return getAsset(user, assetId);
    }

    @Override
    public void unlinkIssue(ApplicationUser user, int assetId, long issueId) {
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                AssetEntity asset = requireVisible(user, assetId);
                Issue issue = issueManager().getIssueObject(Long.valueOf(issueId));
                Project project = project(asset.getProjectKey());
                if (!canWrite(user, project) && !canEditIssue(user, issue)) {
                    throw new AssetException(403, "asset-tree.error.forbidden");
                }
                String before = dropLink(assetId, issueId);
                if (before != null) {
                    applyRemovedFlow(user, asset, issue, before);
                }
                return null;
            }
        });
    }

    @Override
    public List<AssetDto> assetsForIssue(ApplicationUser user, long issueId) {
        requireUser(user);
        Issue issue = issueManager().getIssueObject(Long.valueOf(issueId));
        if (!canBrowse(user, issue)) {
            throw new AssetException(404, "asset-tree.error.issue.notFound");
        }
        return linkedAssets(user, issueId);
    }

    @Override
    public List<AssetDto> searchForIssue(ApplicationUser user, long issueId, String query) {
        Issue issue = issueManager().getIssueObject(Long.valueOf(issueId));
        if (!canBrowse(user, issue) || issue.getProjectObject() == null) {
            throw new AssetException(404, "asset-tree.error.issue.notFound");
        }
        return listAssets(user, issue.getProjectObject().getKey(), query).getAssets();
    }

    @Override
    public IssueContextDto issueContext(ApplicationUser user, long issueId) {
        requireUser(user);
        Issue issue = issueManager().getIssueObject(Long.valueOf(issueId));
        if (!canBrowse(user, issue) || issue.getProjectObject() == null) {
            throw new AssetException(404, "asset-tree.error.issue.notFound");
        }
        Project project = issue.getProjectObject();
        IssueContextDto dto = new IssueContextDto();
        dto.setProjectKey(project.getKey());
        dto.setProjectName(project.getName());
        dto.setCanEdit(canWrite(user, project) || canEditIssue(user, issue));
        return dto;
    }

    @Override
    public ReportDto report(ApplicationUser user, String projectKey) {
        Project project = requireReadableProject(user, projectKey);
        return ao().executeInTransaction(new TransactionCallback<ReportDto>() {
            @Override
            public ReportDto doInTransaction() {
                AssetEntity[] rows = assetsIn(project.getKey());
                List<AssetTypeDto> types = typeDtos(project.getKey(), rows);
                Map<String, AssetTypeDto> typeIndex = indexTypes(types);
                ReportDto report = new ReportDto();
                report.setProjectKey(project.getKey());
                report.setProjectName(project.getName());
                report.setTotal(rows.length);
                List<StatusDto> statuses = statusDtos(project.getKey(), rows);
                Map<String, StatusDto> statusIndex = new LinkedHashMap<String, StatusDto>();
                Map<String, Integer> statusCounts = new LinkedHashMap<String, Integer>();
                for (StatusDto status : statuses) {
                    statusIndex.put(status.getStatusKey(), status);
                    statusCounts.put(status.getStatusKey(), Integer.valueOf(0));
                }
                Map<String, Integer> typeCounts = new LinkedHashMap<String, Integer>();
                Map<String, Integer> holderCounts = new HashMap<String, Integer>();
                Map<Integer, Boolean> locationAsset = new HashMap<Integer, Boolean>();
                int equipment = 0;
                int unassigned = 0;
                for (AssetEntity row : rows) {
                    AssetTypeDto type = typeIndex.get(row.getTypeKey());
                    boolean location = type != null && type.isLocation();
                    locationAsset.put(row.getID(), Boolean.valueOf(location));
                    if (location) {
                        continue;
                    }
                    equipment++;
                    String status = Statuses.canonical(row.getStatus());
                    Integer statusCount = statusCounts.get(status);
                    statusCounts.put(status, Integer.valueOf((statusCount == null ? 0 : statusCount.intValue()) + 1));
                    Integer typeCount = typeCounts.get(row.getTypeKey());
                    typeCounts.put(row.getTypeKey(), Integer.valueOf((typeCount == null ? 0 : typeCount.intValue()) + 1));
                    if (row.getCustodianKey() == null || row.getCustodianKey().isEmpty()) {
                        unassigned++;
                    } else {
                        Integer held = holderCounts.get(row.getCustodianKey());
                        holderCounts.put(row.getCustodianKey(), Integer.valueOf((held == null ? 0 : held.intValue()) + 1));
                    }
                }
                report.setEquipment(equipment);
                report.setUnassigned(unassigned);
                List<ReportBucketDto> byStatus = new ArrayList<ReportBucketDto>();
                for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
                    if (entry.getValue().intValue() == 0) {
                        continue;
                    }
                    StatusDto known = statusIndex.get(entry.getKey());
                    String label = known == null ? statusText(entry.getKey()) : known.getLabel();
                    String color = known == null ? null : categoryColor(known.getCategory());
                    byStatus.add(new ReportBucketDto(entry.getKey(), label, color, entry.getValue().intValue()));
                }
                report.setByStatus(byStatus);
                List<ReportBucketDto> byType = new ArrayList<ReportBucketDto>();
                for (AssetTypeDto type : types) {
                    Integer count = typeCounts.get(type.getTypeKey());
                    if (count == null || count.intValue() == 0) {
                        continue;
                    }
                    ReportBucketDto bucket = new ReportBucketDto(type.getTypeKey(), type.getLabel(), type.getColor(), count.intValue());
                    bucket.setIcon(type.getIcon());
                    byType.add(bucket);
                }
                report.setByType(byType);
                Map<Integer, List<Integer>> children = childrenMap(rows);
                Set<Integer> locationIds = new HashSet<Integer>();
                for (Map.Entry<Integer, Boolean> entry : locationAsset.entrySet()) {
                    if (Boolean.TRUE.equals(entry.getValue())) {
                        locationIds.add(entry.getKey());
                    }
                }
                Map<Integer, Integer> equipmentByPlace = TreeLogic.equipmentCounts(children, locationIds);
                List<ReportPlaceDto> places = new ArrayList<ReportPlaceDto>();
                for (AssetEntity row : rows) {
                    if (!Boolean.TRUE.equals(locationAsset.get(row.getID()))) {
                        continue;
                    }
                    Integer nestedCount = equipmentByPlace.get(row.getID());
                    int nested = nestedCount == null ? 0 : nestedCount.intValue();
                    ReportPlaceDto place = new ReportPlaceDto();
                    place.setId(row.getID());
                    place.setName(row.getName());
                    AssetTypeDto type = typeIndex.get(row.getTypeKey());
                    place.setTypeLabel(type == null ? row.getTypeKey() : type.getLabel());
                    place.setEquipment(nested);
                    places.add(place);
                }
                Collections.sort(places, new Comparator<ReportPlaceDto>() {
                    @Override
                    public int compare(ReportPlaceDto left, ReportPlaceDto right) {
                        return Integer.compare(right.getEquipment(), left.getEquipment());
                    }
                });
                report.setPlaces(places);
                List<ReportHolderDto> holders = new ArrayList<ReportHolderDto>();
                for (Map.Entry<String, Integer> entry : holderCounts.entrySet()) {
                    ReportHolderDto holder = new ReportHolderDto();
                    holder.setUser(profile(entry.getKey()));
                    holder.setCount(entry.getValue().intValue());
                    holders.add(holder);
                }
                Collections.sort(holders, new Comparator<ReportHolderDto>() {
                    @Override
                    public int compare(ReportHolderDto left, ReportHolderDto right) {
                        return Integer.compare(right.getCount(), left.getCount());
                    }
                });
                if (holders.size() > 8) {
                    holders = new ArrayList<ReportHolderDto>(holders.subList(0, 8));
                }
                report.setHolders(holders);
                return report;
            }
        });
    }

    @Override
    public byte[] exportEquipment(ApplicationUser user, String projectKey) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.OBJECT);
        return ao().executeInTransaction(new TransactionCallback<byte[]>() {
            @Override
            public byte[] doInTransaction() {
                return buildEquipmentBook(project.getKey());
            }
        });
    }

    @Override
    public ImportResultDto importEquipment(ApplicationUser user, final String projectKey, final ImportDraft draft) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.OBJECT);
        if (storedAssetPrefix(project.getKey()).isEmpty()) {
            throw new AssetException(400, "asset-tree.error.assetKey.required");
        }
        final List<EquipmentSheet.Sheet> sheets = equipmentSheets(draft);
        return ao().executeInTransaction(new TransactionCallback<ImportResultDto>() {
            @Override
            public ImportResultDto doInTransaction() {
                return applyEquipmentSheets(user, project.getKey(), sheets);
            }
        });
    }

    @Override
    public BulkResultDto applyBulk(ApplicationUser user, String projectKey, BulkDraft draft) {
        Project project = requireReadableProject(user, projectKey);
        if (draft == null || draft.getAction() == null) {
            throw new AssetException(400, "asset-tree.error.bulk.action");
        }
        String action = draft.getAction().trim();
        String target = draft.getTarget() == null ? "" : draft.getTarget().trim();
        if ("type".equals(target)) {
            return bulkTypes(user, project, action, draft.getKeys());
        }
        if (!"asset".equals(target)) {
            throw new AssetException(400, "asset-tree.error.bulk.action");
        }
        return bulkAssets(user, project, action, draft);
    }

    @Override
    public InventoryDto inventory(ApplicationUser user, String projectKey) {
        Project project = requireReadableProject(user, projectKey);
        return ao().executeInTransaction(new TransactionCallback<InventoryDto>() {
            @Override
            public InventoryDto doInTransaction() {
                AssetEntity[] rows = assetsIn(project.getKey());
                Map<String, AssetTypeDto> types = indexTypes(typeDtos(project.getKey(), rows));
                Map<Integer, AssetEntity> index = indexAssets(rows);
                Map<Integer, InventoryMarkEntity> marks = marksFor(idsOf(rows));
                List<InventoryRowDto> list = new ArrayList<InventoryRowDto>();
                int checked = 0;
                for (AssetEntity row : rows) {
                    AssetTypeDto type = types.get(row.getTypeKey());
                    if (type != null && type.isLocation()) {
                        continue;
                    }
                    InventoryRowDto item = inventoryRow(row, type, index, marks.get(Integer.valueOf(row.getID())));
                    if (item.isChecked()) {
                        checked++;
                    }
                    list.add(item);
                }
                Collections.sort(list, new Comparator<InventoryRowDto>() {
                    @Override
                    public int compare(InventoryRowDto left, InventoryRowDto right) {
                        int byPlace = left.getLocation().compareToIgnoreCase(right.getLocation());
                        if (byPlace != 0) {
                            return byPlace;
                        }
                        return left.getName().compareToIgnoreCase(right.getName());
                    }
                });
                InventoryDto dto = new InventoryDto();
                dto.setTotal(list.size());
                dto.setChecked(checked);
                dto.setRows(list);
                return dto;
            }
        });
    }

    @Override
    public InventoryRowDto markInventory(ApplicationUser user, int id, final boolean checked) {
        return ao().executeInTransaction(new TransactionCallback<InventoryRowDto>() {
            @Override
            public InventoryRowDto doInTransaction() {
                AssetEntity entity = requireWritable(user, id);
                InventoryMarkEntity[] existing = ao().find(InventoryMarkEntity.class, Query.select().where("ASSET_ID = ?", entity.getID()));
                InventoryMarkEntity mark = existing.length == 0 ? null : existing[0];
                if (!checked) {
                    if (mark != null) {
                        ao().delete(mark);
                    }
                    for (int i = 1; i < existing.length; i++) {
                        ao().delete(existing[i]);
                    }
                    mark = null;
                } else if (mark == null) {
                    mark = ao().create(InventoryMarkEntity.class,
                            new DBParam("ASSET_ID", entity.getID()),
                            new DBParam("CHECKED_AT", new Date()),
                            new DBParam("CHECKED_BY", user.getKey()));
                } else {
                    mark.setCheckedAt(new Date());
                    mark.setCheckedBy(user.getKey());
                    mark.save();
                    for (int i = 1; i < existing.length; i++) {
                        ao().delete(existing[i]);
                    }
                }
                AssetEntity[] rows = assetsIn(entity.getProjectKey());
                Map<String, AssetTypeDto> types = indexTypes(typeDtos(entity.getProjectKey(), rows));
                return inventoryRow(entity, types.get(entity.getTypeKey()), indexAssets(rows), mark);
            }
        });
    }

    private InventoryRowDto inventoryRow(AssetEntity row, AssetTypeDto type, Map<Integer, AssetEntity> index, InventoryMarkEntity mark) {
        InventoryRowDto item = new InventoryRowDto();
        item.setId(row.getID());
        item.setName(row.getName());
        item.setObjectKey(row.getObjectKey());
        item.setTypeLabel(type == null ? row.getTypeKey() : type.getLabel());
        item.setStatus(Statuses.canonical(row.getStatus()));
        item.setLocation(location(row, index));
        List<Integer> ancestors = new ArrayList<Integer>();
        Integer cursor = TreeLogic.normalizeParent(row.getParentId());
        int guard = 0;
        while (cursor != null && guard < 80) {
            ancestors.add(cursor);
            AssetEntity parent = index.get(cursor);
            if (parent == null) {
                break;
            }
            cursor = TreeLogic.normalizeParent(parent.getParentId());
            guard++;
        }
        Collections.reverse(ancestors);
        item.setAncestors(ancestors);
        if (row.getCustodianKey() != null && !row.getCustodianKey().isEmpty()) {
            UserProfileDto person = profile(row.getCustodianKey());
            item.setCustodian(person == null ? "" : person.getDisplayName());
        } else {
            item.setCustodian("");
        }
        item.setChecked(mark != null);
        item.setCheckedAt(mark == null ? "" : format(mark.getCheckedAt()));
        item.setCheckedBy(mark == null ? "" : displayName(mark.getCheckedBy()));
        return item;
    }

    @Override
    public List<PickerNodeDto> picker(ApplicationUser user, String projectKey) {
        Project project = requireVisibleProject(user, projectKey);
        return ao().executeInTransaction(new TransactionCallback<List<PickerNodeDto>>() {
            @Override
            public List<PickerNodeDto> doInTransaction() {
                AssetEntity[] rows = assetsIn(project.getKey());
                Map<String, AssetTypeDto> types = indexTypes(typeDtos(project.getKey(), rows));
                List<AssetEntity> ordered = new ArrayList<AssetEntity>();
                Collections.addAll(ordered, rows);
                Collections.sort(ordered, SIBLING_ORDER);
                List<PickerLayout.Catalog> catalog = new ArrayList<PickerLayout.Catalog>();
                for (AssetTypeDto type : typeDtos(project.getKey(), rows)) {
                    if (type == null || type.isLocation()) {
                        continue;
                    }
                    String parentKey = type.getParentTypeKey() == null ? "" : type.getParentTypeKey().trim();
                    catalog.add(new PickerLayout.Catalog(type.getTypeKey(), type.getLabel(), parentKey));
                }
                List<PickerLayout.Source> sources = new ArrayList<PickerLayout.Source>();
                for (AssetEntity row : ordered) {
                    AssetTypeDto type = types.get(row.getTypeKey());
                    String parentKey = type == null || type.getParentTypeKey() == null ? "" : type.getParentTypeKey().trim();
                    AssetTypeDto parentType = parentKey.isEmpty() ? null : types.get(parentKey);
                    boolean location = type != null && type.isLocation();
                    sources.add(new PickerLayout.Source(
                            row.getID(),
                            TreeLogic.normalizeParent(row.getParentId()),
                            row.getName(),
                            row.getObjectKey(),
                            Statuses.canonical(row.getStatus()),
                            row.getTypeKey(),
                            type == null ? row.getTypeKey() : type.getLabel(),
                            parentKey,
                            parentType == null ? "" : parentType.getLabel(),
                            location,
                            location ? offeredLines(row.getOfferedTypes()) : null));
                }
                List<PickerNodeDto> nodes = new ArrayList<PickerNodeDto>();
                for (PickerLayout.Node laid : PickerLayout.arrange(sources, catalog)) {
                    PickerNodeDto node = new PickerNodeDto();
                    node.setId(laid.getId());
                    node.setParentId(laid.getParentId());
                    node.setName(laid.getName());
                    node.setObjectKey(laid.getObjectKey());
                    node.setStatus(laid.getStatus());
                    node.setTypeLabel(laid.getTypeLabel());
                    node.setGroup(laid.isGroup());
                    nodes.add(node);
                }
                return nodes;
            }
        });
    }

    @Override
    public List<PortalRuleDto> listPortalRules(ApplicationUser user, String projectKey) {
        final Project project = requireVisibleProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.PORTAL);
        return ao().executeInTransaction(new TransactionCallback<List<PortalRuleDto>>() {
            @Override
            public List<PortalRuleDto> doInTransaction() {
                return portalRules(project.getKey());
            }
        });
    }

    @Override
    public PortalRuleDto addPortalRule(ApplicationUser user, String projectKey, PortalRuleDraft draft) {
        final Project project = requireConfigurableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.PORTAL);
        if (draft == null || draft.getAssetId() <= 0) {
            throw new AssetException(400, "asset-tree.error.portal.target");
        }
        final List<PortalConditionDraft> conditions = cleanConditions(draft.getConditions());
        return ao().executeInTransaction(new TransactionCallback<PortalRuleDto>() {
            @Override
            public PortalRuleDto doInTransaction() {
                AssetEntity asset = ao().get(AssetEntity.class, draft.getAssetId());
                if (asset == null || !ProjectKeys.same(asset.getProjectKey(), project.getKey())) {
                    throw new AssetException(400, "asset-tree.error.portal.target");
                }
                int position = 1;
                for (PortalRuleEntity existing : ao().find(PortalRuleEntity.class, Query.select().where("PROJECT_KEY = ?", project.getKey()))) {
                    if (existing.getPosition() >= position) {
                        position = existing.getPosition() + 1;
                    }
                }
                PortalRuleEntity rule = ao().create(PortalRuleEntity.class,
                        new DBParam("PROJECT_KEY", project.getKey()),
                        new DBParam("ASSET_ID", draft.getAssetId()),
                        new DBParam("POSITION", position));
                for (int i = 0; i < conditions.size(); i++) {
                    PortalConditionDraft condition = conditions.get(i);
                    ao().create(PortalRuleConditionEntity.class,
                            new DBParam("RULE_ID", rule.getID()),
                            new DBParam("POSITION", i),
                            new DBParam("FIELD_NAME", condition.getField()),
                            new DBParam("OPTION_NAME", condition.getOption()));
                }
                return toPortalRule(rule);
            }
        });
    }

    @Override
    public void deletePortalRule(ApplicationUser user, String projectKey, int ruleId) {
        final Project project = requireConfigurableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.PORTAL);
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                PortalRuleEntity rule = ao().get(PortalRuleEntity.class, ruleId);
                if (rule == null || !ProjectKeys.same(rule.getProjectKey(), project.getKey())) {
                    throw new AssetException(404, "asset-tree.error.portal.missing");
                }
                ao().deleteWithSQL(PortalRuleConditionEntity.class, "RULE_ID = ?", ruleId);
                ao().delete(rule);
                return null;
            }
        });
    }

    @Override
    public PortalRuleDto updatePortalRule(ApplicationUser user, String projectKey, final int ruleId, PortalRuleDraft draft) {
        final Project project = requireConfigurableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.PORTAL);
        if (draft == null || draft.getAssetId() <= 0) {
            throw new AssetException(400, "asset-tree.error.portal.target");
        }
        final List<PortalConditionDraft> conditions = cleanConditions(draft.getConditions());
        return ao().executeInTransaction(new TransactionCallback<PortalRuleDto>() {
            @Override
            public PortalRuleDto doInTransaction() {
                PortalRuleEntity rule = ao().get(PortalRuleEntity.class, ruleId);
                if (rule == null || !ProjectKeys.same(rule.getProjectKey(), project.getKey())) {
                    throw new AssetException(404, "asset-tree.error.portal.missing");
                }
                AssetEntity asset = ao().get(AssetEntity.class, draft.getAssetId());
                if (asset == null || !ProjectKeys.same(asset.getProjectKey(), project.getKey())) {
                    throw new AssetException(400, "asset-tree.error.portal.target");
                }
                rule.setAssetId(draft.getAssetId());
                rule.save();
                ao().deleteWithSQL(PortalRuleConditionEntity.class, "RULE_ID = ?", ruleId);
                for (int i = 0; i < conditions.size(); i++) {
                    PortalConditionDraft condition = conditions.get(i);
                    ao().create(PortalRuleConditionEntity.class,
                            new DBParam("RULE_ID", rule.getID()),
                            new DBParam("POSITION", i),
                            new DBParam("FIELD_NAME", condition.getField()),
                            new DBParam("OPTION_NAME", condition.getOption()));
                }
                return toPortalRule(rule);
            }
        });
    }

    @Override
    public List<IssueTypeDto> listIssueTypes(ApplicationUser user, String projectKey) {
        Project project = requireConfigurableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.PORTAL);
        return issueTypesOf(project);
    }

    @Override
    public List<IssueTypeDto> listIssueStatuses(ApplicationUser user, String projectKey) {
        Project project = requireConfigurableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.PORTAL);
        return issueStatusesOf(issueStatusNames(project));
    }

    @Override
    public List<FlowRuleDto> listFlowRules(ApplicationUser user, String projectKey) {
        final Project project = requireConfigurableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.PORTAL);
        final Map<String, String> names = issueTypeNames(project);
        return ao().executeInTransaction(new TransactionCallback<List<FlowRuleDto>>() {
            @Override
            public List<FlowRuleDto> doInTransaction() {
                return flowRules(project.getKey(), names);
            }
        });
    }

    @Override
    public FlowRuleDto saveFlowRule(ApplicationUser user, String projectKey, final FlowRuleDraft draft) {
        final Project project = requireConfigurableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.PORTAL);
        if (draft == null || draft.getIssueTypeId() == null || draft.getIssueTypeId().trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.flow.type");
        }
        if (draft.getStatusKey() == null || draft.getStatusKey().trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.flow.status");
        }
        final String typeId = draft.getIssueTypeId().trim();
        final String typeName = issueTypeNames(project).get(typeId);
        if (typeName == null) {
            throw new AssetException(400, "asset-tree.error.flow.type");
        }
        return ao().executeInTransaction(new TransactionCallback<FlowRuleDto>() {
            @Override
            public FlowRuleDto doInTransaction() {
                String status;
                try {
                    status = resolveStatus(project.getKey(), draft.getStatusKey());
                } catch (AssetException ex) {
                    throw new AssetException(400, "asset-tree.error.flow.status");
                }
                FlowRuleEntity[] existing = ao().find(FlowRuleEntity.class,
                        Query.select().where("PROJECT_KEY = ? AND ISSUE_TYPE_ID = ?", project.getKey(), typeId));
                FlowRuleEntity row;
                if (existing != null && existing.length > 0) {
                    row = existing[0];
                    row.setIssueTypeName(typeName);
                    row.setStatusKey(status);
                    row.save();
                    for (int i = 1; i < existing.length; i++) {
                        ao().delete(existing[i]);
                    }
                } else {
                    row = ao().create(FlowRuleEntity.class,
                            new DBParam("PROJECT_KEY", project.getKey()),
                            new DBParam("ISSUE_TYPE_ID", typeId),
                            new DBParam("ISSUE_TYPE_NAME", typeName),
                            new DBParam("STATUS_KEY", status));
                }
                return toFlow(row);
            }
        });
    }

    @Override
    public void deleteFlowRule(ApplicationUser user, String projectKey, final int ruleId) {
        final Project project = requireConfigurableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.PORTAL);
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                FlowRuleEntity rule = ao().get(FlowRuleEntity.class, ruleId);
                if (rule == null || !ProjectKeys.same(rule.getProjectKey(), project.getKey())) {
                    throw new AssetException(404, "asset-tree.error.flow.missing");
                }
                ao().delete(rule);
                return null;
            }
        });
    }

    @Override
    public FlowBookDto flowGraph(ApplicationUser user, String projectKey) {
        final Project project = requireConfigurableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.PORTAL);
        final Map<String, String> names = issueTypeNames(project);
        final Map<String, String> issueStatuses = issueStatusNames(project);
        return ao().executeInTransaction(new TransactionCallback<FlowBookDto>() {
            @Override
            public FlowBookDto doInTransaction() {
                FlowGraphEntity stored = findFlowGraph(project.getKey());
                List<FlowProcessDto> processes;
                if (stored != null && stored.getBody() != null && !stored.getBody().trim().isEmpty()) {
                    processes = FlowBook.read(stored.getBody());
                } else {
                    processes = new ArrayList<FlowProcessDto>();
                    FlowGraphDto graph = FlowGraph.fromRules(flowRules(project.getKey(), names));
                    if (!graph.getNodes().isEmpty()) {
                        FlowProcessDto one = new FlowProcessDto();
                        one.setId("rules1");
                        one.setNodes(graph.getNodes());
                        one.setLinks(graph.getLinks());
                        processes.add(one);
                    }
                }
                FlowBookDto book = FlowBook.book(processes);
                nameFlowBook(book, names, issueStatuses);
                return book;
            }
        });
    }

    @Override
    public FlowBookDto saveFlowGraph(ApplicationUser user, String projectKey, FlowBookDto draft) {
        final Project project = requireConfigurableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.PORTAL);
        final Map<String, String> names = issueTypeNames(project);
        final Map<String, String> issueStatuses = issueStatusNames(project);
        final FlowBookDto book = FlowBook.book(draft == null ? null : draft.getProcesses());
        for (FlowProcessDto process : book.getProcesses()) {
            for (FlowNodeDto node : process.getNodes()) {
                if (FlowGraph.REQUEST.equals(node.getKind()) && node.getIssueTypeId() != null
                        && !node.getIssueTypeId().isEmpty() && !names.containsKey(node.getIssueTypeId())) {
                    throw new AssetException(400, "asset-tree.error.flow.type");
                }
                if (FlowGraph.ISSUE.equals(node.getKind()) && node.getIssueStatusId() != null
                        && !node.getIssueStatusId().isEmpty() && !issueStatuses.containsKey(node.getIssueStatusId())) {
                    throw new AssetException(400, "asset-tree.error.flow.issue");
                }
            }
        }
        return ao().executeInTransaction(new TransactionCallback<FlowBookDto>() {
            @Override
            public FlowBookDto doInTransaction() {
                for (FlowProcessDto process : book.getProcesses()) {
                    for (FlowNodeDto node : process.getNodes()) {
                        if (!FlowGraph.STATUS.equals(node.getKind()) || node.getStatusKey() == null || node.getStatusKey().isEmpty()) {
                            continue;
                        }
                        try {
                            node.setStatusKey(resolveStatus(project.getKey(), node.getStatusKey()));
                        } catch (AssetException ex) {
                            throw new AssetException(400, "asset-tree.error.flow.status");
                        }
                    }
                }
                String body = FlowBook.write(book.getProcesses());
                FlowGraphEntity stored = findFlowGraph(project.getKey());
                if (stored == null) {
                    ao().create(FlowGraphEntity.class,
                            new DBParam("PROJECT_KEY", project.getKey()),
                            new DBParam("BODY", body));
                } else {
                    stored.setBody(body);
                    stored.save();
                }
                ao().deleteWithSQL(FlowRuleEntity.class, "PROJECT_KEY = ?", project.getKey());
                for (FlowRuleDto pair : FlowBook.pairs(book.getProcesses())) {
                    String typeName = names.get(pair.getIssueTypeId());
                    if (typeName == null) {
                        continue;
                    }
                    ao().create(FlowRuleEntity.class,
                            new DBParam("PROJECT_KEY", project.getKey()),
                            new DBParam("ISSUE_TYPE_ID", pair.getIssueTypeId()),
                            new DBParam("ISSUE_TYPE_NAME", typeName),
                            new DBParam("STATUS_KEY", pair.getStatusKey()));
                }
                nameFlowBook(book, names, issueStatuses);
                return book;
            }
        });
    }

    @Override
    public CardLayout cardLayout(ApplicationUser user, String projectKey) {
        Project project = requireReadableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.CARD);
        return ao().executeInTransaction(new TransactionCallback<CardLayout>() {
            @Override
            public CardLayout doInTransaction() {
                return layoutOf(findCard(project.getKey()));
            }
        });
    }

    @Override
    public CardLayout saveCardLayout(ApplicationUser user, String projectKey, final CardLayout draft) {
        final Project project = requireConfigurableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.CARD);
        final CardLayout layout = CardLayout.normalize(draft);
        return ao().executeInTransaction(new TransactionCallback<CardLayout>() {
            @Override
            public CardLayout doInTransaction() {
                CardTemplateEntity row = findCard(project.getKey());
                if (row == null) {
                    row = ao().create(CardTemplateEntity.class, new DBParam("PROJECT_KEY", project.getKey()));
                }
                row.setCardSize(layout.getSize());
                row.setWidthMm(layout.getWidth());
                row.setHeightMm(layout.getHeight());
                row.setGridMm(layout.getGrid());
                row.setQrPlace(layout.getQr());
                row.setLines(CardLayout.join(layout.getLines()));
                row.setCanvas(layout.getElements() == null ? null : CardCanvas.write(layout.getElements()));
                row.save();
                return layout;
            }
        });
    }

    @Override
    public AssetKeyDto saveAssetKey(ApplicationUser user, String projectKey, AssetKeyDto draft) {
        final Project project = requireArrangeProject(user, projectKey);
        final String prefix = AssetKeyRules.normalize(draft == null ? "" : draft.getAssetKey());
        String error = AssetKeyRules.invalid(prefix);
        if (error != null) {
            throw new AssetException(400, error);
        }
        Collection<Project> known = projectManager().getProjects();
        if (known != null) {
            for (Project item : known) {
                if (item != null && item.getKey() != null && prefix.equalsIgnoreCase(item.getKey())) {
                    throw new AssetException(400, "asset-tree.error.assetKey.project");
                }
            }
        }
        return ao().executeInTransaction(new TransactionCallback<AssetKeyDto>() {
            @Override
            public AssetKeyDto doInTransaction() {
                ProjectAssetKeyEntity[] rows = ao().find(ProjectAssetKeyEntity.class);
                ProjectAssetKeyEntity current = null;
                if (rows != null) {
                    for (ProjectAssetKeyEntity row : rows) {
                        if (row.getAssetPrefix() != null && prefix.equalsIgnoreCase(row.getAssetPrefix())
                                && !ProjectKeys.same(row.getProjectKey(), project.getKey())) {
                            throw new AssetException(400, "asset-tree.error.assetKey.taken");
                        }
                        if (current == null && ProjectKeys.same(row.getProjectKey(), project.getKey())) {
                            current = row;
                        }
                    }
                }
                AssetEntity[] existing = ao().find(AssetEntity.class, Query.select().where("OBJECT_KEY LIKE ?", prefix + "-%"));
                if (existing != null) {
                    for (AssetEntity asset : existing) {
                        if (ObjectKeys.suffix(prefix, asset.getObjectKey()) <= 0) {
                            continue;
                        }
                        if (!ProjectKeys.same(asset.getProjectKey(), project.getKey())) {
                            throw new AssetException(400, "asset-tree.error.assetKey.taken");
                        }
                    }
                }
                if (current == null) {
                    ao().create(ProjectAssetKeyEntity.class,
                            new DBParam("PROJECT_KEY", project.getKey()),
                            new DBParam("ASSET_PREFIX", prefix));
                } else if (!prefix.equals(current.getAssetPrefix())) {
                    current.setAssetPrefix(prefix);
                    current.save();
                }
                AssetKeyDto saved = new AssetKeyDto();
                saved.setAssetKey(prefix);
                return saved;
            }
        });
    }

    @Override
    public ProjectViewDto saveProjectView(ApplicationUser user, String projectKey, ProjectViewDto draft) {
        final Project project = requireArrangeProject(user, projectKey);
        final Boolean tree = draft == null ? null : draft.getTreeObjects();
        final List<String> columns = draft == null || draft.getColumns() == null ? null : ViewColumns.clean(draft.getColumns());
        final List<String> modules = draft == null ? null : draft.getModules();
        return ao().executeInTransaction(new TransactionCallback<ProjectViewDto>() {
            @Override
            public ProjectViewDto doInTransaction() {
                ProjectViewEntity row = viewRow(project.getKey());
                if (row == null) {
                    row = ao().create(ProjectViewEntity.class,
                            new DBParam("PROJECT_KEY", project.getKey()),
                            new DBParam("TREE_MODE", Integer.valueOf(0)));
                }
                if (tree != null) {
                    row.setTreeMode(tree.booleanValue() ? 1 : 0);
                }
                if (columns != null) {
                    row.setColumnList(ViewColumns.join(columns));
                }
                if (modules != null) {
                    row.setModuleList(ProjectModules.join(modules));
                }
                if (draft != null && draft.getSetup() != null) {
                    row.setOptionList(ModuleSetup.write(draft.getSetup()));
                }
                row.save();
                return viewDto(row);
            }
        });
    }

    private ProjectViewDto viewDto(ProjectViewEntity row) {
        ProjectViewDto saved = new ProjectViewDto();
        saved.setTreeObjects(Boolean.valueOf(row != null && row.getTreeMode() == 1));
        saved.setColumns(row == null ? new ArrayList<String>() : ViewColumns.split(row.getColumnList()));
        saved.setModules(row == null ? new ArrayList<String>() : ProjectModules.split(row.getModuleList()));
        saved.setSetup(ModuleSetup.read(row == null ? "" : row.getOptionList()));
        return saved;
    }

    private boolean moduleOn(String projectKey, String module) {
        ProjectViewEntity row = viewRow(projectKey);
        return row != null && ProjectModules.has(row.getModuleList(), module);
    }

    private ProjectViewEntity viewRow(String projectKey) {
        ProjectViewEntity[] rows = ao().find(ProjectViewEntity.class);
        if (rows != null) {
            for (ProjectViewEntity row : rows) {
                if (ProjectKeys.same(row.getProjectKey(), projectKey)) {
                    return row;
                }
            }
        }
        return null;
    }

    private void attachViews(List<ProjectDto> projects) {
        Map<String, ProjectViewEntity> stored = new HashMap<String, ProjectViewEntity>();
        try {
            ProjectViewEntity[] rows = ao().executeInTransaction(new TransactionCallback<ProjectViewEntity[]>() {
                @Override
                public ProjectViewEntity[] doInTransaction() {
                    return ao().find(ProjectViewEntity.class);
                }
            });
            if (rows != null) {
                for (ProjectViewEntity row : rows) {
                    if (row.getProjectKey() != null) {
                        stored.put(row.getProjectKey(), row);
                    }
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Project views were not loaded", ex);
        }
        for (ProjectDto project : projects) {
            ProjectViewEntity row = stored.get(project.getKey());
            project.setTreeObjects(row != null && row.getTreeMode() == 1);
            project.setColumns(row == null ? new ArrayList<String>() : ViewColumns.split(row.getColumnList()));
            project.setModules(row == null ? new ArrayList<String>() : ProjectModules.split(row.getModuleList()));
            project.setSetup(ModuleSetup.read(row == null ? "" : row.getOptionList()));
        }
    }

    private void attachAssetKeys(List<ProjectDto> projects) {
        Map<String, String> stored = new HashMap<String, String>();
        try {
            List<ProjectAssetKeyEntity> rows = ao().executeInTransaction(new TransactionCallback<List<ProjectAssetKeyEntity>>() {
                @Override
                public List<ProjectAssetKeyEntity> doInTransaction() {
                    ProjectAssetKeyEntity[] found = ao().find(ProjectAssetKeyEntity.class);
                    List<ProjectAssetKeyEntity> list = new ArrayList<ProjectAssetKeyEntity>();
                    if (found != null) {
                        Collections.addAll(list, found);
                    }
                    return list;
                }
            });
            for (ProjectAssetKeyEntity row : rows) {
                if (row.getProjectKey() != null && row.getAssetPrefix() != null) {
                    stored.put(row.getProjectKey(), row.getAssetPrefix());
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Asset keys were not loaded", ex);
        }
        for (ProjectDto project : projects) {
            String prefix = stored.get(project.getKey());
            project.setAssetKey(prefix == null ? "" : prefix);
        }
    }

    private void attachCards(List<ProjectDto> projects) {
        Map<String, CardLayout> stored = new HashMap<String, CardLayout>();
        try {
            List<CardTemplateEntity> rows = ao().executeInTransaction(new TransactionCallback<List<CardTemplateEntity>>() {
                @Override
                public List<CardTemplateEntity> doInTransaction() {
                    CardTemplateEntity[] found = ao().find(CardTemplateEntity.class);
                    List<CardTemplateEntity> list = new ArrayList<CardTemplateEntity>();
                    if (found != null) {
                        Collections.addAll(list, found);
                    }
                    return list;
                }
            });
            for (CardTemplateEntity row : rows) {
                stored.put(row.getProjectKey(), layoutOf(row));
            }
        } catch (RuntimeException ex) {
            log.warn("Card layouts were not loaded", ex);
        }
        for (ProjectDto project : projects) {
            CardLayout layout = stored.get(project.getKey());
            project.setCard(layout == null ? CardLayout.defaults() : layout);
        }
    }

    private CardTemplateEntity findCard(String projectKey) {
        CardTemplateEntity[] found = ao().find(CardTemplateEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        return found == null || found.length == 0 ? null : found[0];
    }

    private CardLayout layoutOf(CardTemplateEntity row) {
        if (row == null) {
            return CardLayout.defaults();
        }
        CardLayout raw = new CardLayout();
        raw.setSize(row.getCardSize());
        raw.setWidth(row.getWidthMm());
        raw.setHeight(row.getHeightMm());
        raw.setGrid(row.getGridMm());
        raw.setQr(row.getQrPlace());
        raw.setLines(CardLayout.split(row.getLines()));
        String canvas = row.getCanvas();
        if (canvas != null && !canvas.trim().isEmpty()) {
            raw.setElements(CardCanvas.read(canvas));
        }
        return CardLayout.normalize(raw);
    }

    private List<PortalConditionDraft> cleanConditions(List<PortalConditionDraft> raw) {
        List<PortalConditionDraft> conditions = new ArrayList<PortalConditionDraft>();
        if (raw != null) {
            for (PortalConditionDraft item : raw) {
                if (item == null) {
                    continue;
                }
                String field = item.getField() == null ? "" : item.getField().trim().replaceAll("\\s+", " ");
                String option = item.getOption() == null ? "" : item.getOption().trim().replaceAll("\\s+", " ");
                if (field.isEmpty() || option.isEmpty()) {
                    continue;
                }
                if (field.length() > 255 || option.length() > 255) {
                    throw new AssetException(400, "asset-tree.error.portal.field");
                }
                boolean duplicate = false;
                for (PortalConditionDraft kept : conditions) {
                    if (kept.getField().equalsIgnoreCase(field)) {
                        duplicate = true;
                    }
                }
                if (duplicate) {
                    throw new AssetException(400, "asset-tree.error.portal.field");
                }
                PortalConditionDraft clean = new PortalConditionDraft();
                clean.setField(field);
                clean.setOption(option);
                conditions.add(clean);
                if (conditions.size() > 6) {
                    throw new AssetException(400, "asset-tree.error.portal.field");
                }
            }
        }
        if (conditions.isEmpty()) {
            throw new AssetException(400, "asset-tree.error.portal.conditions");
        }
        return conditions;
    }

    private List<PortalRuleDto> portalRules(String projectKey) {
        PortalRuleEntity[] rows = ao().find(PortalRuleEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        Arrays.sort(rows, new Comparator<PortalRuleEntity>() {
            @Override
            public int compare(PortalRuleEntity left, PortalRuleEntity right) {
                int byPosition = left.getPosition() - right.getPosition();
                if (byPosition != 0) {
                    return byPosition;
                }
                return left.getID() - right.getID();
            }
        });
        List<PortalRuleDto> result = new ArrayList<PortalRuleDto>();
        for (PortalRuleEntity row : rows) {
            result.add(toPortalRule(row));
        }
        return result;
    }

    private PortalRuleDto toPortalRule(PortalRuleEntity rule) {
        PortalRuleDto dto = new PortalRuleDto();
        dto.setId(rule.getID());
        dto.setAssetId(rule.getAssetId());
        dto.setPosition(rule.getPosition());
        AssetEntity asset = ao().get(AssetEntity.class, rule.getAssetId());
        if (asset != null) {
            dto.setAssetName(asset.getName());
            dto.setAssetPath(portalPath(asset));
        }
        PortalRuleConditionEntity[] conditions = ao().find(PortalRuleConditionEntity.class,
                Query.select().where("RULE_ID = ?", rule.getID()));
        Arrays.sort(conditions, new Comparator<PortalRuleConditionEntity>() {
            @Override
            public int compare(PortalRuleConditionEntity left, PortalRuleConditionEntity right) {
                return left.getPosition() - right.getPosition();
            }
        });
        List<PortalConditionDraft> items = new ArrayList<PortalConditionDraft>();
        for (PortalRuleConditionEntity condition : conditions) {
            PortalConditionDraft item = new PortalConditionDraft();
            item.setField(condition.getFieldName());
            item.setOption(condition.getOptionName());
            items.add(item);
        }
        dto.setConditions(items);
        return dto;
    }

    private List<IssueTypeDto> issueTypesOf(Project project) {
        List<IssueTypeDto> result = new ArrayList<IssueTypeDto>();
        Map<String, String> names = issueTypeNames(project);
        for (Map.Entry<String, String> entry : names.entrySet()) {
            IssueTypeDto dto = new IssueTypeDto();
            dto.setId(entry.getKey());
            dto.setName(entry.getValue());
            result.add(dto);
        }
        Collections.sort(result, new Comparator<IssueTypeDto>() {
            @Override
            public int compare(IssueTypeDto left, IssueTypeDto right) {
                String leftName = left.getName() == null ? "" : left.getName();
                String rightName = right.getName() == null ? "" : right.getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return result;
    }

    private Map<String, String> issueTypeNames(Project project) {
        Map<String, String> names = new LinkedHashMap<String, String>();
        if (project == null || project.getIssueTypes() == null) {
            return names;
        }
        for (IssueType type : project.getIssueTypes()) {
            if (type == null || type.isSubTask() || type.getId() == null || type.getId().trim().isEmpty()) {
                continue;
            }
            String name = type.getName() == null || type.getName().trim().isEmpty() ? type.getId().trim() : type.getName().trim();
            names.put(type.getId().trim(), name);
        }
        return names;
    }

    private List<FlowRuleDto> flowRules(String projectKey, Map<String, String> names) {
        FlowRuleEntity[] rows = ao().find(FlowRuleEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        if (rows == null) {
            return new ArrayList<FlowRuleDto>();
        }
        Arrays.sort(rows, new Comparator<FlowRuleEntity>() {
            @Override
            public int compare(FlowRuleEntity left, FlowRuleEntity right) {
                String leftName = left.getIssueTypeName() == null ? "" : left.getIssueTypeName();
                String rightName = right.getIssueTypeName() == null ? "" : right.getIssueTypeName();
                int byName = leftName.compareToIgnoreCase(rightName);
                if (byName != 0) {
                    return byName;
                }
                return left.getID() - right.getID();
            }
        });
        List<FlowRuleDto> result = new ArrayList<FlowRuleDto>();
        for (FlowRuleEntity row : rows) {
            FlowRuleDto dto = toFlow(row);
            String live = names == null ? null : names.get(dto.getIssueTypeId());
            if (live != null) {
                dto.setIssueTypeName(live);
            }
            result.add(dto);
        }
        return result;
    }

    private FlowRuleDto toFlow(FlowRuleEntity rule) {
        FlowRuleDto dto = new FlowRuleDto();
        dto.setId(rule.getID());
        dto.setIssueTypeId(rule.getIssueTypeId());
        dto.setIssueTypeName(rule.getIssueTypeName());
        dto.setStatusKey(rule.getStatusKey());
        return dto;
    }

    private FlowGraphEntity findFlowGraph(String projectKey) {
        FlowGraphEntity[] found = ao().find(FlowGraphEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        return found == null || found.length == 0 ? null : found[0];
    }

    private void nameFlowBook(FlowBookDto book, Map<String, String> names, Map<String, String> issueStatuses) {
        if (book == null || book.getProcesses() == null) {
            return;
        }
        for (FlowProcessDto process : book.getProcesses()) {
            FlowGraphDto graph = new FlowGraphDto();
            graph.setNodes(process.getNodes());
            graph.setLinks(process.getLinks());
            nameFlowNodes(graph, names, issueStatuses);
        }
    }

    private void nameFlowNodes(FlowGraphDto graph, Map<String, String> names, Map<String, String> issueStatuses) {
        if (graph == null || graph.getNodes() == null) {
            return;
        }
        for (FlowNodeDto node : graph.getNodes()) {
            if (node == null) {
                continue;
            }
            if (FlowGraph.REQUEST.equals(node.getKind()) && names != null) {
                String live = names.get(node.getIssueTypeId());
                if (live != null) {
                    node.setIssueTypeName(live);
                }
            }
            if (FlowGraph.ISSUE.equals(node.getKind()) && issueStatuses != null) {
                String live = issueStatuses.get(node.getIssueStatusId());
                if (live != null) {
                    node.setIssueStatusName(live);
                }
            }
        }
    }

    private List<IssueTypeDto> issueStatusesOf(Map<String, String> names) {
        List<IssueTypeDto> result = new ArrayList<IssueTypeDto>();
        if (names == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : names.entrySet()) {
            IssueTypeDto dto = new IssueTypeDto();
            dto.setId(entry.getKey());
            dto.setName(entry.getValue());
            result.add(dto);
        }
        Collections.sort(result, new Comparator<IssueTypeDto>() {
            @Override
            public int compare(IssueTypeDto left, IssueTypeDto right) {
                String leftName = left.getName() == null ? "" : left.getName();
                String rightName = right.getName() == null ? "" : right.getName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return result;
    }

    private Map<String, String> issueStatusNames(Project project) {
        Map<String, String> names = new LinkedHashMap<String, String>();
        try {
            WorkflowManager workflows = ComponentAccessor.getWorkflowManager();
            if (workflows != null && project != null && project.getIssueTypes() != null) {
                for (IssueType type : project.getIssueTypes()) {
                    if (type == null || type.isSubTask() || type.getId() == null || type.getId().trim().isEmpty()) {
                        continue;
                    }
                    JiraWorkflow workflow = workflows.getWorkflow(project.getId(), type.getId());
                    if (workflow == null || workflow.getLinkedStatusObjects() == null) {
                        continue;
                    }
                    for (Status status : workflow.getLinkedStatusObjects()) {
                        rememberStatus(names, status);
                    }
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Workflow statuses were not loaded for {}", project == null ? "" : project.getKey(), ex);
        }
        if (!names.isEmpty()) {
            return names;
        }
        try {
            ConstantsManager constants = ComponentAccessor.getConstantsManager();
            if (constants != null && constants.getStatuses() != null) {
                for (Status status : constants.getStatuses()) {
                    rememberStatus(names, status);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("Issue statuses were not loaded", ex);
        }
        return names;
    }

    private void rememberStatus(Map<String, String> names, Status status) {
        if (status == null || status.getId() == null || status.getId().trim().isEmpty()) {
            return;
        }
        String name = status.getName() == null || status.getName().trim().isEmpty() ? status.getId().trim() : status.getName().trim();
        names.put(status.getId().trim(), name);
    }

    private void applyLinkedFlow(ApplicationUser user, int assetId, Issue issue) {
        try {
            applyRequestFlow(user, assetId, issue);
        } catch (RuntimeException ex) {
            log.warn("Could not apply the equipment flow for {}", assetId, ex);
        }
    }

    private void applyRequestFlow(ApplicationUser user, final int assetId, Issue issue) {
        if (issue == null || issue.getIssueType() == null || issue.getProjectObject() == null) {
            return;
        }
        final String typeId = issue.getIssueType().getId();
        if (typeId == null || typeId.trim().isEmpty()) {
            return;
        }
        final String projectKey = issue.getProjectObject().getKey();
        final String wanted = typeId.trim();
        final String issueStatusId = issue.getStatus() == null || issue.getStatus().getId() == null
                ? "" : issue.getStatus().getId().trim();
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                AssetEntity asset = ao().get(AssetEntity.class, assetId);
                if (asset == null || !ProjectKeys.same(asset.getProjectKey(), projectKey) || isLocation(asset)) {
                    return null;
                }
                applyResolvedStatus(user, asset, wanted, equipmentStatusFor(projectKey, wanted, null, false));
                if (!issueStatusId.isEmpty()) {
                    applyResolvedStatus(user, asset, wanted, equipmentStatusFor(projectKey, wanted, issueStatusId, true));
                }
                return null;
            }
        });
    }

    /** Deletes the link and returns the status the asset had when it was linked, or null when there was no link. */
    private String dropLink(int assetId, long issueId) {
        AssetIssueLinkEntity[] links = ao().find(AssetIssueLinkEntity.class,
                Query.select().where("ASSET_ID = ? AND ISSUE_ID = ?", assetId, issueId));
        if (links == null || links.length == 0) {
            return null;
        }
        String before = "";
        for (AssetIssueLinkEntity link : links) {
            if (before.isEmpty() && link.getStatusBefore() != null) {
                before = link.getStatusBefore().trim();
            }
        }
        ao().deleteWithSQL(AssetIssueLinkEntity.class, "ASSET_ID = ? AND ISSUE_ID = ?", assetId, issueId);
        return before;
    }

    private void applyRemovedFlow(ApplicationUser user, AssetEntity asset, Issue issue, String statusBefore) {
        if (asset == null || issue == null || issue.getProjectObject() == null || isLocation(asset)) {
            return;
        }
        String projectKey = issue.getProjectObject().getKey();
        if (!ProjectKeys.same(asset.getProjectKey(), projectKey)) {
            return;
        }
        FlowGraphEntity stored = findFlowGraph(projectKey);
        if (stored == null || stored.getBody() == null || stored.getBody().trim().isEmpty()) {
            return;
        }
        String typeId = issue.getIssueType() == null || issue.getIssueType().getId() == null
                ? "" : issue.getIssueType().getId().trim();
        String next = FlowBook.removedStatus(FlowBook.read(stored.getBody()), typeId);
        if (FlowBook.RESTORE.equals(next)) {
            next = statusBefore == null || statusBefore.isEmpty() ? null : statusBefore;
        }
        if (next == null || !knownStatusKeys(projectKey).contains(next)
                || next.equals(Statuses.canonical(asset.getStatus()))) {
            return;
        }
        assignFlowStatus(user, asset, next);
    }

    private void applyResolvedStatus(ApplicationUser user, AssetEntity asset, String issueTypeId, String resolved) {
        if (resolved == null || resolved.isEmpty()) {
            return;
        }
        FlowRuleDto rule = new FlowRuleDto();
        rule.setIssueTypeId(issueTypeId);
        rule.setStatusKey(resolved);
        String next = FlowRules.statusToApply(Collections.singletonList(rule), issueTypeId, true,
                Statuses.canonical(asset.getStatus()), knownStatusKeys(asset.getProjectKey()));
        if (next == null) {
            return;
        }
        assignFlowStatus(user, asset, next);
    }

    private String equipmentStatusFor(String projectKey, String issueTypeId, String issueStatusId, boolean onTransition) {
        FlowGraphEntity stored = findFlowGraph(projectKey);
        if (stored != null && stored.getBody() != null && !stored.getBody().trim().isEmpty()) {
            return FlowBook.equipmentStatus(FlowBook.read(stored.getBody()), issueTypeId, issueStatusId, onTransition);
        }
        if (onTransition || issueTypeId == null || issueTypeId.trim().isEmpty()) {
            return null;
        }
        FlowRuleEntity[] rows = ao().find(FlowRuleEntity.class,
                Query.select().where("PROJECT_KEY = ? AND ISSUE_TYPE_ID = ?", projectKey, issueTypeId.trim()));
        if (rows == null || rows.length == 0) {
            return null;
        }
        Arrays.sort(rows, new Comparator<FlowRuleEntity>() {
            @Override
            public int compare(FlowRuleEntity left, FlowRuleEntity right) {
                return left.getID() - right.getID();
            }
        });
        String found = null;
        for (FlowRuleEntity row : rows) {
            if (row.getStatusKey() != null && !row.getStatusKey().trim().isEmpty()) {
                found = row.getStatusKey().trim();
            }
        }
        return found;
    }

    private Set<String> knownStatusKeys(String projectKey) {
        ensureStatuses(projectKey);
        Set<String> known = new HashSet<String>();
        ProjectStatusEntity[] statuses = projectStatuses(projectKey);
        if (statuses != null) {
            for (ProjectStatusEntity status : statuses) {
                if (status.getStatusKey() != null) {
                    known.add(status.getStatusKey());
                }
            }
        }
        return known;
    }

    private void assignFlowStatus(ApplicationUser user, AssetEntity asset, String next) {
        String oldStatus = Statuses.canonical(asset.getStatus());
        asset.setStatus(next);
        asset.setUpdated(new Date());
        asset.setUpdatedBy(user == null ? "" : user.getKey());
        asset.save();
        logActivity(asset.getID(), user == null ? "" : user.getKey(), "status", "", oldStatus, next);
    }

    private String portalPath(AssetEntity entity) {
        List<String> names = new ArrayList<String>();
        AssetEntity cursor = entity;
        int guard = 0;
        while (cursor != null && guard < 40) {
            names.add(0, cursor.getName());
            Integer parentId = TreeLogic.normalizeParent(cursor.getParentId());
            cursor = parentId == null ? null : ao().get(AssetEntity.class, parentId.intValue());
            guard++;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                builder.append(" / ");
            }
            builder.append(names.get(i));
        }
        return builder.toString();
    }

    @Override
    public List<UserProfileDto> searchUsers(ApplicationUser user, String query) {
        requireUser(user);
        String needle = query == null ? "" : query.trim();
        if (needle.length() < 2) {
            return Collections.emptyList();
        }
        List<ApplicationUser> found = userSearchService().findUsers(needle, UserSearchParams.LIMITED_ACTIVE_USERS_IGNORE_EMPTY_QUERY);
        List<UserProfileDto> profiles = new ArrayList<UserProfileDto>();
        if (found == null) {
            return profiles;
        }
        for (ApplicationUser match : found) {
            profiles.add(profile(match.getKey()));
            if (profiles.size() >= 8) {
                break;
            }
        }
        return profiles;
    }

    @Override
    public List<AssetDto> assetsForUser(ApplicationUser user, String userKey) {
        requireUser(user);
        if (userKey == null || userKey.trim().isEmpty() || userManager().getUserByKey(userKey.trim()) == null) {
            throw new AssetException(404, "asset-tree.error.user");
        }
        final String holder = userKey.trim();
        final List<ServiceNotice> notices = new ArrayList<ServiceNotice>();
        List<AssetDto> result = ao().executeInTransaction(new TransactionCallback<List<AssetDto>>() {
            @Override
            public List<AssetDto> doInTransaction() {
                List<AssetDto> found = new ArrayList<AssetDto>();
                for (ProjectDto project : visibleProjects(user)) {
                    AssetEntity[] rows = assetsIn(project.getKey());
                    applyDueFor(project.getKey(), rows, notices);
                    Map<String, AssetTypeDto> types = indexTypes(typeDtos(project.getKey(), rows));
                    Map<Integer, AssetEntity> index = indexAssets(rows);
                    Map<Integer, List<AssetAttributeEntity>> attributes = attributesByAsset(idsOf(rows));
                    for (AssetEntity row : rows) {
                        AssetTypeDto type = types.get(row.getTypeKey());
                        if (type != null && type.isLocation()) {
                            continue;
                        }
                        String role = holderRole(row, attributes.get(row.getID()), type, holder);
                        if (role == null) {
                            continue;
                        }
                        AssetDto dto = toDto(row, attributes.get(row.getID()), types, index, false);
                        dto.setHolderRole(role);
                        dto.setProjectName(project.getName());
                        found.add(dto);
                    }
                }
                Collections.sort(found, new Comparator<AssetDto>() {
                    @Override
                    public int compare(AssetDto left, AssetDto right) {
                        String leftProject = left.getProjectName() == null ? "" : left.getProjectName();
                        String rightProject = right.getProjectName() == null ? "" : right.getProjectName();
                        int byProject = leftProject.compareToIgnoreCase(rightProject);
                        if (byProject != 0) {
                            return byProject;
                        }
                        return left.getName().compareToIgnoreCase(right.getName());
                    }
                });
                return found;
            }
        });
        dispatchNotices(notices);
        return result;
    }

    @Override
    public void syncRequestAsset(ApplicationUser user, Issue issue, String previousAssetId, String nextAssetId) {
        if (user == null || issue == null || issue.getProjectObject() == null || !canBrowse(user, issue)) {
            return;
        }
        Integer previous = parseId(previousAssetId);
        Integer next = parseId(nextAssetId);
        if (previous != null && (next == null || previous.intValue() != next.intValue())) {
            try {
                ao().executeInTransaction(new TransactionCallback<Void>() {
                    @Override
                    public Void doInTransaction() {
                        String before = dropLink(previous.intValue(), issue.getId().longValue());
                        if (before != null) {
                            applyRemovedFlow(user, ao().get(AssetEntity.class, previous.intValue()), issue, before);
                        }
                        return null;
                    }
                });
            } catch (RuntimeException ex) {
                log.warn("Could not drop the previous asset link {}", previous, ex);
            }
        }
        if (next == null) {
            return;
        }
        boolean fresh = previous == null || previous.intValue() != next.intValue();
        try {
            link(user, next.intValue(), issue, false);
        } catch (AssetException ex) {
            if (ex.getStatus() != 409) {
                log.warn("Could not link asset {} to {}", next, issue.getKey());
            }
            return;
        }
        if (!fresh) {
            return;
        }
        try {
            applyRequestFlow(user, next.intValue(), issue);
        } catch (RuntimeException ex) {
            log.warn("Could not apply the equipment flow for {}", next, ex);
        }
    }

    @Override
    public void syncRequestStatus(ApplicationUser user, Issue issue) {
        if (issue == null || issue.getId() == null || issue.getProjectObject() == null || issue.getStatus() == null) {
            return;
        }
        final String projectKey = issue.getProjectObject().getKey();
        final String typeId = issue.getIssueType() == null || issue.getIssueType().getId() == null
                ? "" : issue.getIssueType().getId().trim();
        final String issueStatusId = issue.getStatus().getId() == null ? "" : issue.getStatus().getId().trim();
        if (projectKey == null || projectKey.trim().isEmpty() || issueStatusId.isEmpty()) {
            return;
        }
        final long issueId = issue.getId().longValue();
        try {
            ao().executeInTransaction(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction() {
                    String resolved = equipmentStatusFor(projectKey, typeId, issueStatusId, true);
                    if (resolved == null || resolved.isEmpty()) {
                        return null;
                    }
                    Set<String> known = knownStatusKeys(projectKey);
                    String matchType = typeId.isEmpty() ? "*" : typeId;
                    FlowRuleDto rule = new FlowRuleDto();
                    rule.setIssueTypeId(matchType);
                    rule.setStatusKey(resolved);
                    AssetIssueLinkEntity[] links = ao().find(AssetIssueLinkEntity.class,
                            Query.select().where("ISSUE_ID = ?", issueId));
                    if (links == null) {
                        return null;
                    }
                    for (AssetIssueLinkEntity link : links) {
                        AssetEntity asset = ao().get(AssetEntity.class, link.getAssetId());
                        if (asset == null || !ProjectKeys.same(asset.getProjectKey(), projectKey) || isLocation(asset)) {
                            continue;
                        }
                        String next = FlowRules.statusToApply(Collections.singletonList(rule), matchType, true,
                                Statuses.canonical(asset.getStatus()), known);
                        if (next == null) {
                            continue;
                        }
                        assignFlowStatus(user, asset, next);
                    }
                    return null;
                }
            });
        } catch (RuntimeException ex) {
            log.warn("Could not apply the equipment flow for issue {}", issue.getKey(), ex);
        }
    }

    @Override
    public String describeAsset(String assetId) {
        Integer id = parseId(assetId);
        if (id == null) {
            return assetId == null ? "" : assetId;
        }
        AssetEntity entity = ao().get(AssetEntity.class, id.intValue());
        if (entity == null) {
            return assetId;
        }
        ApplicationUser user = authenticationContext().getLoggedInUser();
        Project project = project(entity.getProjectKey());
        if (user == null || !canSee(user, project)) {
            return entity.getObjectKey();
        }
        return entity.getObjectKey() + "  " + entity.getName();
    }

    private IssueRefDto link(ApplicationUser user, int assetId, Issue issue, boolean requireWrite) {
        return ao().executeInTransaction(new TransactionCallback<IssueRefDto>() {
            @Override
            public IssueRefDto doInTransaction() {
                AssetEntity asset = requireWrite ? requireWritable(user, assetId) : requireVisible(user, assetId);
                if (issue.getProjectObject() == null || !ProjectKeys.same(asset.getProjectKey(), issue.getProjectObject().getKey())) {
                    throw new AssetException(400, "asset-tree.error.project.mismatch");
                }
                AssetIssueLinkEntity[] existing = ao().find(AssetIssueLinkEntity.class,
                        Query.select().where("ASSET_ID = ? AND ISSUE_ID = ?", assetId, issue.getId()));
                if (existing.length > 0) {
                    throw new AssetException(409, "asset-tree.error.issue.duplicate");
                }
                ao().create(AssetIssueLinkEntity.class,
                        new DBParam("ASSET_ID", assetId),
                        new DBParam("ISSUE_ID", issue.getId()),
                        new DBParam("STATUS_BEFORE", Statuses.canonical(asset.getStatus())));
                return toIssue(issue);
            }
        });
    }

    private List<AssetDto> linkedAssets(ApplicationUser user, long issueId) {
        return ao().executeInTransaction(new TransactionCallback<List<AssetDto>>() {
            @Override
            public List<AssetDto> doInTransaction() {
                AssetIssueLinkEntity[] links = ao().find(AssetIssueLinkEntity.class, Query.select().where("ISSUE_ID = ?", issueId));
                List<AssetEntity> visible = new ArrayList<AssetEntity>();
                Map<String, AssetEntity[]> rowsByProject = new HashMap<String, AssetEntity[]>();
                if (links != null) {
                    for (AssetIssueLinkEntity link : links) {
                        AssetEntity entity = ao().get(AssetEntity.class, link.getAssetId());
                        if (entity == null) {
                            continue;
                        }
                        Project project = project(entity.getProjectKey());
                        if (!canOpen(user, project)) {
                            continue;
                        }
                        visible.add(entity);
                        if (!rowsByProject.containsKey(entity.getProjectKey())) {
                            rowsByProject.put(entity.getProjectKey(), assetsIn(entity.getProjectKey()));
                        }
                    }
                }
                Map<String, Map<String, AssetTypeDto>> typesByProject = new HashMap<String, Map<String, AssetTypeDto>>();
                Map<String, Map<Integer, AssetEntity>> indexByProject = new HashMap<String, Map<Integer, AssetEntity>>();
                Map<String, Map<Integer, List<AssetAttributeEntity>>> attributesByProject =
                        new HashMap<String, Map<Integer, List<AssetAttributeEntity>>>();
                for (Map.Entry<String, AssetEntity[]> entry : rowsByProject.entrySet()) {
                    AssetEntity[] rows = entry.getValue();
                    typesByProject.put(entry.getKey(), indexTypes(typeDtos(entry.getKey(), rows)));
                    indexByProject.put(entry.getKey(), indexAssets(rows));
                    attributesByProject.put(entry.getKey(), attributesByAsset(idsOf(rows)));
                }
                List<AssetDto> assets = new ArrayList<AssetDto>();
                for (AssetEntity entity : visible) {
                    String projectKey = entity.getProjectKey();
                    Map<Integer, List<AssetAttributeEntity>> attributes = attributesByProject.get(projectKey);
                    List<AssetAttributeEntity> stored = attributes == null ? null : attributes.get(Integer.valueOf(entity.getID()));
                    assets.add(toDto(entity, stored, typesByProject.get(projectKey), indexByProject.get(projectKey), false));
                }
                Collections.sort(assets, new Comparator<AssetDto>() {
                    @Override
                    public int compare(AssetDto left, AssetDto right) {
                        return left.getName().compareToIgnoreCase(right.getName());
                    }
                });
                return assets;
            }
        });
    }

    private List<EquipmentSheet.Sheet> equipmentSheets(ImportDraft draft) {
        String content = draft == null ? null : draft.getContent();
        if (content != null && !content.trim().isEmpty()) {
            if (content.length() > 4_000_000) {
                throw new AssetException(400, "asset-tree.error.import.limit", Integer.valueOf(EquipmentExchange.MAX_ROWS));
            }
            byte[] bytes;
            try {
                bytes = java.util.Base64.getMimeDecoder().decode(content.trim());
            } catch (IllegalArgumentException ex) {
                throw new AssetException(400, "asset-tree.error.import.workbook");
            }
            if (bytes.length > 2_000_000) {
                throw new AssetException(400, "asset-tree.error.import.limit", Integer.valueOf(EquipmentExchange.MAX_ROWS));
            }
            return WorkbookSheet.readAll(bytes);
        }
        String csv = draft == null || draft.getCsv() == null ? "" : draft.getCsv();
        if (csv.length() > 1_500_000) {
            throw new AssetException(400, "asset-tree.error.import.limit", Integer.valueOf(EquipmentExchange.MAX_ROWS));
        }
        return Collections.singletonList(EquipmentSheet.read(csv));
    }

    private byte[] buildEquipmentBook(String projectKey) {
        ensureStatuses(projectKey);
        AssetEntity[] rows = assetsIn(projectKey);
        List<AssetTypeDto> types = typeDtos(projectKey, rows);
        Map<Integer, AssetEntity> index = indexAssets(rows);
        List<StatusDto> statuses = statusDtos(projectKey, rows);
        Map<String, String> statusLabels = new HashMap<String, String>();
        for (StatusDto status : statuses) {
            statusLabels.put(status.getStatusKey(), status.getLabel());
        }
        I18nHelper labels = i18n();
        boolean dayFirst = labels.getLocale() != null && "ru".equalsIgnoreCase(labels.getLocale().getLanguage());
        List<String> common = new ArrayList<String>();
        common.add(labels.getText("asset-tree.ui.keyLabel"));
        common.add(labels.getText("asset-tree.ui.name"));
        common.add(labels.getText("asset-tree.ui.type"));
        common.add(labels.getText("asset-tree.ui.status"));
        common.add(labels.getText("asset-tree.ui.exchangePlace"));
        common.add(labels.getText("asset-tree.ui.custodian"));
        common.add(labels.getText("asset-tree.ui.description"));
        Map<String, List<AssetEntity>> byType = new LinkedHashMap<String, List<AssetEntity>>();
        for (AssetTypeDto type : types) {
            if (!type.isLocation()) {
                byType.put(type.getTypeKey(), new ArrayList<AssetEntity>());
            }
        }
        for (AssetEntity row : rows) {
            List<AssetEntity> bucket = byType.get(row.getTypeKey());
            if (bucket != null) {
                bucket.add(row);
            }
        }
        Comparator<AssetEntity> byKey = new Comparator<AssetEntity>() {
            @Override
            public int compare(AssetEntity left, AssetEntity right) {
                String leftKey = left.getObjectKey() == null ? "" : left.getObjectKey();
                String rightKey = right.getObjectKey() == null ? "" : right.getObjectKey();
                return leftKey.compareToIgnoreCase(rightKey);
            }
        };
        List<WorkbookSheet.Page> pages = new ArrayList<WorkbookSheet.Page>();
        for (AssetTypeDto type : types) {
            if (type.isLocation()) {
                continue;
            }
            List<String> headers = new ArrayList<String>(common);
            List<FieldDto> columns = new ArrayList<FieldDto>();
            Set<String> taken = new HashSet<String>();
            for (String header : headers) {
                taken.add(EquipmentExchange.norm(header));
            }
            if (type.getFields() != null) {
                for (FieldDto field : type.getFields()) {
                    String marker = EquipmentExchange.norm(field.getLabel());
                    if (marker.isEmpty() || !taken.add(marker)) {
                        continue;
                    }
                    columns.add(field);
                    headers.add(field.getLabel());
                }
            }
            List<AssetEntity> equipment = byType.get(type.getTypeKey());
            if (equipment == null) {
                equipment = new ArrayList<AssetEntity>();
            }
            Collections.sort(equipment, byKey);
            List<List<String>> lines = new ArrayList<List<String>>();
            for (AssetEntity row : equipment) {
                Map<String, String> values = attributeValues(row.getID());
                List<String> line = new ArrayList<String>();
                line.add(row.getObjectKey());
                line.add(row.getName());
                line.add(type.getLabel());
                String status = Statuses.canonical(row.getStatus());
                line.add(statusLabels.containsKey(status) ? statusLabels.get(status) : status);
                line.add(location(row, index));
                line.add(displayName(row.getCustodianKey()));
                line.add(row.getDescription() == null ? "" : row.getDescription());
                for (FieldDto field : columns) {
                    String value = values.containsKey(field.getFieldKey()) ? values.get(field.getFieldKey()) : "";
                    if (FieldKinds.USER.equals(field.getKind()) && value != null && !value.isEmpty()) {
                        value = displayName(value);
                    }
                    if (FieldKinds.DATE.equals(field.getKind())) {
                        value = FieldDates.display(value, dayFirst);
                    }
                    if (FieldChoices.handles(field.getKind())) {
                        value = FieldChoices.display(value);
                    }
                    line.add(value == null ? "" : value);
                }
                lines.add(line);
            }
            pages.add(new WorkbookSheet.Page(type.getLabel(), headers, lines));
        }
        if (pages.isEmpty()) {
            pages.add(new WorkbookSheet.Page("Equipment", common, new ArrayList<List<String>>()));
        }
        return WorkbookSheet.write(pages);
    }

    private ImportResultDto applyEquipmentSheets(ApplicationUser user, String projectKey, List<EquipmentSheet.Sheet> sheets) {
        ensureStatuses(projectKey);
        AssetEntity[] rows = assetsIn(projectKey);
        List<AssetTypeDto> typeDtos = typeDtos(projectKey, rows);
        List<EquipmentExchange.TypeRef> types = new ArrayList<EquipmentExchange.TypeRef>();
        for (AssetTypeDto type : typeDtos) {
            List<EquipmentExchange.FieldRef> fields = new ArrayList<EquipmentExchange.FieldRef>();
            if (type.getFields() != null) {
                for (FieldDto field : type.getFields()) {
                    String options = field.getOptions() == null ? "" : String.join("\n", field.getOptions());
                    fields.add(new EquipmentExchange.FieldRef(field.getFieldKey(), field.getLabel(), field.getKind(), field.isRequired(), options));
                }
            }
            types.add(new EquipmentExchange.TypeRef(type.getTypeKey(), type.getLabel(), type.isLocation(), fields));
        }
        List<StatusDto> statusDtos = statusDtos(projectKey, rows);
        List<EquipmentExchange.Named> statuses = new ArrayList<EquipmentExchange.Named>();
        for (StatusDto status : statusDtos) {
            statuses.add(new EquipmentExchange.Named(status.getStatusKey(), status.getLabel()));
        }
        Map<String, Boolean> locationByType = new HashMap<String, Boolean>();
        for (AssetTypeDto type : typeDtos) {
            locationByType.put(type.getTypeKey(), Boolean.valueOf(type.isLocation()));
        }
        List<EquipmentExchange.Node> nodes = new ArrayList<EquipmentExchange.Node>();
        Map<Integer, AssetEntity> index = indexAssets(rows);
        for (AssetEntity row : rows) {
            Boolean location = locationByType.get(row.getTypeKey());
            nodes.add(new EquipmentExchange.Node(row.getID(), TreeLogic.normalizeParent(row.getParentId()), row.getName(),
                    row.getObjectKey(), location != null && location.booleanValue()));
        }
        I18nHelper labels = i18n();
        Map<String, String> headerLabels = new LinkedHashMap<String, String>();
        headerLabels.put("key", labels.getText("asset-tree.ui.keyLabel"));
        headerLabels.put("name", labels.getText("asset-tree.ui.name"));
        headerLabels.put("type", labels.getText("asset-tree.ui.type"));
        headerLabels.put("status", labels.getText("asset-tree.ui.status"));
        headerLabels.put("place", labels.getText("asset-tree.ui.exchangePlace"));
        headerLabels.put("custodian", labels.getText("asset-tree.ui.custodian"));
        headerLabels.put("description", labels.getText("asset-tree.ui.description"));
        EquipmentExchange.Plan plan = EquipmentExchange.planBook(sheets, types, statuses, nodes,
                Statuses.IN_USE, headerLabels, new EquipmentExchange.Directory() {
                    @Override
                    public EquipmentExchange.Person find(String raw) {
                        return lookupPerson(raw);
                    }
                });
        ImportResultDto result = new ImportResultDto();
        List<ImportIssueDto> errors = new ArrayList<ImportIssueDto>();
        for (EquipmentExchange.Issue issue : plan.getIssues()) {
            errors.add(translateIssue(labels, issue));
        }
        int created = 0;
        int updated = 0;
        boolean headerOnly = false;
        for (EquipmentExchange.Issue issue : plan.getIssues()) {
            if (issue.getRow() <= 1 && plan.getChanges().isEmpty()) {
                headerOnly = true;
            }
        }
        if (!headerOnly) {
            for (EquipmentExchange.Change change : plan.getChanges()) {
                try {
                    if (change.getExistingId() == null) {
                        createImported(user, projectKey, change);
                        created++;
                    } else {
                        updateImported(user, change, index);
                        updated++;
                    }
                } catch (AssetException ex) {
                    String text = ex.getArgs() == null || ex.getArgs().length == 0
                            ? labels.getText(ex.getMessageKey())
                            : labels.getText(ex.getMessageKey(), ex.getArgs());
                    errors.add(new ImportIssueDto(change.getRow(), text));
                }
            }
        }
        result.setCreated(created);
        result.setUpdated(updated);
        result.setErrors(errors);
        return result;
    }

    private BulkResultDto bulkTypes(ApplicationUser user, Project project, String action, List<String> keys) {
        if (!"delete".equals(action)) {
            throw new AssetException(400, "asset-tree.error.bulk.action");
        }
        List<String> unique = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        if (keys != null) {
            for (String key : keys) {
                if (key == null || key.trim().isEmpty() || !seen.add(key.trim())) {
                    continue;
                }
                unique.add(key.trim());
            }
        }
        if (unique.isEmpty()) {
            throw new AssetException(400, "asset-tree.error.bulk.empty");
        }
        if (unique.size() > BulkSelection.MAX_ITEMS) {
            throw new AssetException(400, "asset-tree.error.bulk.limit", Integer.valueOf(BulkSelection.MAX_ITEMS));
        }
        BulkResultDto result = new BulkResultDto();
        List<BulkIssueDto> errors = new ArrayList<BulkIssueDto>();
        int done = 0;
        I18nHelper labels = i18n();
        for (String key : unique) {
            AssetTypeEntity type = findType(key);
            String label = type == null ? key : type.getLabel();
            try {
                if (type != null && !ProjectKeys.same(type.getProjectKey(), project.getKey())) {
                    throw new AssetException(400, "asset-tree.error.project.mismatch");
                }
                deleteType(user, key);
                done++;
            } catch (AssetException ex) {
                errors.add(new BulkIssueDto(label, exceptionText(labels, ex)));
            }
        }
        result.setDone(done);
        result.setErrors(errors);
        return result;
    }

    private BulkResultDto bulkAssets(ApplicationUser user, Project project, String action, BulkDraft draft) {
        if (!"delete".equals(action) && !"move".equals(action) && !"status".equals(action)
                && !"custodian".equals(action) && !"copy".equals(action)) {
            throw new AssetException(400, "asset-tree.error.bulk.action");
        }
        if ("move".equals(action) && !draft.isToRoot() && TreeLogic.normalizeParent(draft.getParentId()) == null) {
            throw new AssetException(400, "asset-tree.error.import.place.required");
        }
        LinkedHashSet<Integer> unique = new LinkedHashSet<Integer>();
        if (draft.getIds() != null) {
            for (Integer id : draft.getIds()) {
                if (id != null && id.intValue() > 0) {
                    unique.add(id);
                }
            }
        }
        if (unique.isEmpty()) {
            throw new AssetException(400, "asset-tree.error.bulk.empty");
        }
        if (unique.size() > BulkSelection.MAX_ITEMS) {
            throw new AssetException(400, "asset-tree.error.bulk.limit", Integer.valueOf(BulkSelection.MAX_ITEMS));
        }
        List<Integer> ids = new ArrayList<Integer>(unique);
        AssetEntity[] rows = assetsIn(project.getKey());
        Map<Integer, Integer> parents = parentMap(rows);
        boolean outermost = "move".equals(action) || ("delete".equals(action) && draft.isCascade());
        List<Integer> order = outermost ? BulkSelection.keepRoots(ids, parents) : BulkSelection.deepestFirst(ids, parents);
        Set<Integer> selected = new HashSet<Integer>(ids);
        Set<Integer> covered = new HashSet<Integer>();
        BulkResultDto result = new BulkResultDto();
        List<BulkIssueDto> errors = new ArrayList<BulkIssueDto>();
        I18nHelper labels = i18n();
        for (Integer id : order) {
            String label = assetLabel(id);
            try {
                AssetEntity entity = ao().get(AssetEntity.class, id.intValue());
                if (entity == null) {
                    if (outermost) {
                        covered.add(id);
                    }
                    continue;
                }
                if (!ProjectKeys.same(entity.getProjectKey(), project.getKey())) {
                    throw new AssetException(400, "asset-tree.error.project.mismatch");
                }
                if ("delete".equals(action)) {
                    deleteAsset(user, id.intValue(), draft.isCascade());
                } else if ("move".equals(action)) {
                    MoveDraft move = new MoveDraft();
                    move.setParentId(draft.isToRoot() ? null : draft.getParentId());
                    moveAsset(user, id.intValue(), move);
                } else if ("status".equals(action)) {
                    patchStatus(user, id.intValue(), draft.getStatus());
                } else if ("copy".equals(action)) {
                    copyAsset(user, id.intValue());
                } else {
                    patchCustodian(user, id.intValue(), draft.getCustodianKey());
                }
                covered.add(id);
                if (outermost) {
                    for (Integer other : selected) {
                        if (BulkSelection.isUnder(other.intValue(), id.intValue(), parents)) {
                            covered.add(other);
                        }
                    }
                }
            } catch (AssetException ex) {
                errors.add(new BulkIssueDto(label, exceptionText(labels, ex)));
            }
        }
        result.setDone(covered.size());
        result.setErrors(errors);
        return result;
    }

    private void patchStatus(ApplicationUser user, final int id, final String status) {
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                AssetEntity entity = requireAssetCap(user, id, kindCap(requireAsset(id)));
                if (isLocation(entity)) {
                    throw new AssetException(400, "asset-tree.error.bulk.place", entity.getName());
                }
                String oldStatus = Statuses.canonical(entity.getStatus());
                String next = resolveStatus(entity.getProjectKey(), status);
                entity.setStatus(next);
                entity.setUpdated(new Date());
                entity.setUpdatedBy(user.getKey());
                entity.save();
                logActivity(id, user.getKey(), "status", "", oldStatus, next);
                return null;
            }
        });
    }

    private void patchCustodian(ApplicationUser user, final int id, final String custodianKey) {
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                AssetEntity entity = requireAssetCap(user, id, kindCap(requireAsset(id)));
                if (isLocation(entity)) {
                    throw new AssetException(400, "asset-tree.error.bulk.place", entity.getName());
                }
                String oldCustodian = entity.getCustodianKey() == null ? "" : entity.getCustodianKey();
                String next = normalizeCustodian(custodianKey);
                entity.setCustodianKey(next);
                entity.setUpdated(new Date());
                entity.setUpdatedBy(user.getKey());
                entity.save();
                logActivity(id, user.getKey(), "custodian", "", displayName(oldCustodian), displayName(next == null ? "" : next));
                return null;
            }
        });
    }

    private Integer placeContainer(Integer startId, AssetEntity[] rows, Set<Integer> skip) {
        Map<Integer, Boolean> locations = new HashMap<Integer, Boolean>();
        if (rows != null) {
            for (AssetEntity row : rows) {
                locations.put(Integer.valueOf(row.getID()), Boolean.valueOf(isLocation(row)));
            }
        }
        return PlaceHistory.container(startId, locations, parentMap(rows == null ? new AssetEntity[0] : rows), skip);
    }

    private void logPlaceChange(ApplicationUser user, Integer placeId, String kind, AssetEntity child) {
        if (user == null || placeId == null || child == null) {
            return;
        }
        String key = child.getObjectKey() == null ? "" : child.getObjectKey();
        String name = child.getName() == null ? "" : child.getName();
        boolean arrival = "place_add".equals(kind) || "place_in".equals(kind);
        recordActivity(placeId.intValue(), user.getKey(), kind, key, arrival ? "" : name, arrival ? name : "");
    }

    private boolean isLocation(AssetEntity entity) {
        AssetTypeEntity type = entity == null ? null : findType(entity.getTypeKey());
        return type != null && type.isLocation();
    }

    private String assetLabel(int id) {
        AssetEntity entity = ao().get(AssetEntity.class, id);
        if (entity == null || entity.getName() == null || entity.getName().trim().isEmpty()) {
            return String.valueOf(id);
        }
        return entity.getName();
    }

    private String exceptionText(I18nHelper labels, AssetException ex) {
        String text = ex.getArgs() == null || ex.getArgs().length == 0
                ? labels.getText(ex.getMessageKey())
                : labels.getText(ex.getMessageKey(), ex.getArgs());
        if (text == null || text.equals(ex.getMessageKey())) {
            return ex.getMessageKey();
        }
        return text;
    }

    private void createImported(ApplicationUser user, String projectKey, EquipmentExchange.Change change) {
        Date now = new Date();
        Integer parentId = Integer.valueOf(change.getParentId());
        AssetEntity entity = ao().create(AssetEntity.class,
                new DBParam("NAME", change.getName()),
                new DBParam("OBJECT_KEY", nextKey(projectKey)),
                new DBParam("DESCRIPTION", change.getDescription() == null ? "" : change.getDescription()),
                new DBParam("TYPE_KEY", change.getTypeKey()),
                new DBParam("STATUS", change.getStatusKey()),
                new DBParam("SORT_ORDER", nextSort(parentId, projectKey)),
                new DBParam("CREATED", now),
                new DBParam("UPDATED", now),
                new DBParam("CREATED_BY", user.getKey()),
                new DBParam("UPDATED_BY", user.getKey()),
                new DBParam("PROJECT_KEY", projectKey));
        entity.setParentId(parentId);
        entity.setCustodianKey(change.getCustodianKey());
        entity.save();
        replaceAttributes(entity, drafts(change));
        logActivity(entity.getID(), user.getKey(), "created", "", "", entity.getName());
        logPlaceChange(user, placeContainer(parentId, assetsIn(projectKey), null), "place_add", entity);
    }

    private void updateImported(ApplicationUser user, EquipmentExchange.Change change, Map<Integer, AssetEntity> index) {
        AssetEntity entity = index.get(change.getExistingId());
        if (entity == null) {
            entity = ao().get(AssetEntity.class, change.getExistingId().intValue());
        }
        if (entity == null) {
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        String oldName = entity.getName() == null ? "" : entity.getName();
        String oldDescription = entity.getDescription() == null ? "" : entity.getDescription();
        String oldType = entity.getTypeKey() == null ? "" : entity.getTypeKey();
        String oldStatus = Statuses.canonical(entity.getStatus());
        String oldCustodian = entity.getCustodianKey() == null ? "" : entity.getCustodianKey();
        Integer oldParent = TreeLogic.normalizeParent(entity.getParentId());
        AssetEntity[] projectRows = assetsIn(entity.getProjectKey());
        Integer fromPlace = placeContainer(oldParent, projectRows, null);
        Integer toPlace = placeContainer(Integer.valueOf(change.getParentId()), projectRows, null);
        Map<String, String> oldAttributes = attributeValues(entity.getID());
        String newDescription = change.getDescription() == null ? oldDescription : change.getDescription();
        String newCustodian = change.isCustodianPresent()
                ? (change.getCustodianKey() == null ? "" : change.getCustodianKey())
                : oldCustodian;
        entity.setName(change.getName());
        entity.setDescription(newDescription);
        entity.setTypeKey(change.getTypeKey());
        entity.setStatus(change.getStatusKey());
        entity.setCustodianKey(newCustodian.isEmpty() ? null : newCustodian);
        entity.setParentId(Integer.valueOf(change.getParentId()));
        entity.setUpdated(new Date());
        entity.setUpdatedBy(user.getKey());
        entity.save();
        replaceAttributes(entity, drafts(change, oldAttributes));
        int id = entity.getID();
        logActivity(id, user.getKey(), "name", "", oldName, change.getName());
        logActivity(id, user.getKey(), "description", "", clip(oldDescription), clip(newDescription));
        logActivity(id, user.getKey(), "type", "", oldType, change.getTypeKey());
        logActivity(id, user.getKey(), "status", "", oldStatus, change.getStatusKey());
        logActivity(id, user.getKey(), "custodian", "", displayName(oldCustodian), displayName(newCustodian));
        if (oldParent == null || oldParent.intValue() != change.getParentId()) {
            AssetEntity previous = oldParent == null ? null : index.get(oldParent);
            AssetEntity next = index.get(Integer.valueOf(change.getParentId()));
            logActivity(id, user.getKey(), "move", "", previous == null ? "" : previous.getName(), next == null ? "" : next.getName());
        }
        if (fromPlace != null && !fromPlace.equals(toPlace)) {
            logPlaceChange(user, fromPlace, "place_out", entity);
        }
        if (toPlace != null && !toPlace.equals(fromPlace)) {
            logPlaceChange(user, toPlace, "place_in", entity);
        }
        logAttributeChanges(id, user.getKey(), change.getTypeKey(), oldAttributes);
    }

    private List<AttributeDraft> drafts(EquipmentExchange.Change change) {
        return drafts(change, null);
    }

    private List<AttributeDraft> drafts(EquipmentExchange.Change change, Map<String, String> kept) {
        List<AttributeDraft> attributes = new ArrayList<AttributeDraft>();
        Set<String> present = new HashSet<String>();
        for (EquipmentExchange.Value value : change.getAttributes()) {
            AttributeDraft draft = new AttributeDraft();
            draft.setFieldKey(value.getFieldKey());
            draft.setValue(value.getValue());
            attributes.add(draft);
            present.add(value.getFieldKey());
        }
        if (kept != null) {
            for (Map.Entry<String, String> entry : kept.entrySet()) {
                if (present.contains(entry.getKey())) {
                    continue;
                }
                AttributeDraft draft = new AttributeDraft();
                draft.setFieldKey(entry.getKey());
                draft.setValue(entry.getValue());
                attributes.add(draft);
            }
        }
        return attributes;
    }

    private ImportIssueDto translateIssue(I18nHelper labels, EquipmentExchange.Issue issue) {
        String text = issue.getArgument() == null || issue.getArgument().isEmpty()
                ? labels.getText(issue.getMessageKey())
                : labels.getText(issue.getMessageKey(), issue.getArgument());
        if (text == null || text.equals(issue.getMessageKey())) {
            text = issue.getMessageKey();
        }
        if (issue.getSheet() != null && !issue.getSheet().isEmpty()) {
            text = issue.getSheet() + ": " + text;
        }
        return new ImportIssueDto(issue.getRow(), text);
    }

    private EquipmentExchange.Person lookupPerson(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return EquipmentExchange.Person.blank();
        }
        String text = raw.trim();
        ApplicationUser direct = userManager().getUserByKey(text);
        if (direct == null) {
            direct = userManager().getUserByName(text);
        }
        if (direct != null) {
            return EquipmentExchange.Person.found(direct.getKey());
        }
        if (text.length() < 2) {
            return EquipmentExchange.Person.missing();
        }
        List<ApplicationUser> found = userSearchService().findUsers(text, UserSearchParams.LIMITED_ACTIVE_USERS_IGNORE_EMPTY_QUERY);
        List<ApplicationUser> exact = new ArrayList<ApplicationUser>();
        if (found != null) {
            for (ApplicationUser match : found) {
                if (samePerson(match, text)) {
                    exact.add(match);
                }
            }
        }
        if (exact.size() == 1) {
            return EquipmentExchange.Person.found(exact.get(0).getKey());
        }
        if (exact.size() > 1) {
            return EquipmentExchange.Person.ambiguous();
        }
        return EquipmentExchange.Person.missing();
    }

    private boolean samePerson(ApplicationUser match, String text) {
        if (match == null) {
            return false;
        }
        return text.equalsIgnoreCase(match.getName())
                || text.equalsIgnoreCase(match.getKey())
                || (match.getEmailAddress() != null && text.equalsIgnoreCase(match.getEmailAddress()))
                || (match.getDisplayName() != null && text.equalsIgnoreCase(match.getDisplayName()));
    }

    private String nextKey(String projectKey) {
        String prefix = storedAssetPrefix(projectKey);
        if (prefix.isEmpty()) {
            throw new AssetException(400, "asset-tree.error.assetKey.required");
        }
        Object created = new Object();
        Object raced = KEY_LOCKS.putIfAbsent(prefix, created);
        Object lock = raced == null ? created : raced;
        synchronized (lock) {
            for (int attempt = 0; attempt < 8; attempt++) {
                AssetCounterEntity counter = counterFor(prefix);
                int value = Math.max(counter.getNextValue(), 1);
                counter.setNextValue(value + 1);
                counter.save();
                String key = prefix + "-" + value;
                if (key.length() <= 32 && ao().count(AssetEntity.class, "OBJECT_KEY = ?", key) == 0) {
                    return key;
                }
            }
        }
        throw new AssetException(500, "asset-tree.error.unexpected");
    }

    private String storedAssetPrefix(String projectKey) {
        if (projectKey == null || projectKey.isEmpty()) {
            return "";
        }
        ProjectAssetKeyEntity[] rows = ao().find(ProjectAssetKeyEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        if (rows == null || rows.length == 0 || rows[0].getAssetPrefix() == null) {
            return "";
        }
        return rows[0].getAssetPrefix();
    }

    private AssetCounterEntity counterFor(String prefix) {
        AssetCounterEntity[] rows = ao().find(AssetCounterEntity.class, Query.select().where("PROJECT_KEY = ?", prefix));
        if (rows.length > 0) {
            return rows[0];
        }
        int start = 1;
        AssetEntity[] existing = ao().find(AssetEntity.class, Query.select().where("OBJECT_KEY LIKE ?", prefix + "-%"));
        if (existing != null) {
            for (AssetEntity row : existing) {
                int suffix = ObjectKeys.suffix(prefix, row.getObjectKey());
                if (suffix >= start) {
                    start = suffix + 1;
                }
            }
        }
        return ao().create(AssetCounterEntity.class,
                new DBParam("NEXT_VALUE", start),
                new DBParam("PROJECT_KEY", prefix));
    }

    private int nextSort(Integer parentId, String projectKey) {
        int max = -1;
        for (AssetEntity asset : assetsIn(projectKey)) {
            if (TreeLogic.sameParent(asset.getParentId(), parentId) && asset.getSortOrder() > max) {
                max = asset.getSortOrder();
            }
        }
        return max + 1;
    }

    private void replaceAttributes(AssetEntity entity, List<AttributeDraft> attributes) {
        Map<String, AssetFieldEntity> schema = new LinkedHashMap<String, AssetFieldEntity>();
        for (AssetFieldEntity field : fields(entity.getTypeKey())) {
            schema.put(field.getFieldKey(), field);
        }
        ao().deleteWithSQL(AssetAttributeEntity.class, "ASSET_ID = ?", entity.getID());
        Map<String, String> values = new HashMap<String, String>();
        if (attributes != null) {
            for (AttributeDraft attribute : attributes) {
                if (attribute == null || attribute.getFieldKey() == null) {
                    continue;
                }
                String fieldKey = attribute.getFieldKey().trim();
                if (!schema.containsKey(fieldKey) || values.containsKey(fieldKey)) {
                    continue;
                }
                values.put(fieldKey, attribute.getValue() == null ? "" : attribute.getValue());
            }
        }
        I18nHelper labels = i18n();
        int position = 0;
        for (AssetFieldEntity field : schema.values()) {
            String value = values.containsKey(field.getFieldKey()) ? values.get(field.getFieldKey()) : "";
            String trimmed = value == null ? "" : value.trim();
            if (field.isRequired() && trimmed.isEmpty()) {
                throw new AssetException(400, "asset-tree.error.field.required", fieldLabel(field, labels));
            }
            if (!trimmed.isEmpty() && FieldKinds.NUMBER.equals(field.getKind()) && !NUMBER.matcher(trimmed).matches()) {
                throw new AssetException(400, "asset-tree.error.number");
            }
            if (!trimmed.isEmpty() && FieldKinds.DATE.equals(field.getKind())) {
                String canonical = FieldDates.canonical(trimmed);
                if (canonical == null) {
                    throw new AssetException(400, "asset-tree.error.date");
                }
                value = canonical;
            }
            if (!trimmed.isEmpty() && FieldKinds.USER.equals(field.getKind()) && userManager().getUserByKey(trimmed) == null) {
                throw new AssetException(400, "asset-tree.error.user");
            }
            if (!trimmed.isEmpty() && FieldChoices.handles(field.getKind())) {
                String normalized = FieldChoices.canonicalValue(field.getKind(), field.getOptions(), trimmed);
                if (normalized == null) {
                    throw new AssetException(400, FieldChoices.errorKey(field.getKind()));
                }
                value = normalized;
            }
            if (trimmed.length() > AssetValidator.MAX_ATTR_VALUE) {
                throw new AssetException(400, "asset-tree.error.attribute.value.length");
            }
            ao().create(AssetAttributeEntity.class,
                    new DBParam("ASSET_ID", entity.getID()),
                    new DBParam("ATTR_NAME", field.getFieldKey()),
                    new DBParam("ATTR_VALUE", value == null ? "" : value),
                    new DBParam("ATTR_POSITION", position));
            position++;
        }
    }

    private String normalizeCustodian(String userKey) {
        if (userKey == null || userKey.trim().isEmpty()) {
            return null;
        }
        ApplicationUser holder = userManager().getUserByKey(userKey.trim());
        if (holder == null) {
            throw new AssetException(400, "asset-tree.error.user");
        }
        return holder.getKey();
    }

    private String holderRole(AssetEntity asset, List<AssetAttributeEntity> attributes, AssetTypeDto type, String userKey) {
        if (userKey.equals(asset.getCustodianKey())) {
            return i18n().getText("asset-tree.ui.custodian");
        }
        if (attributes == null || type == null || type.getFields() == null) {
            return null;
        }
        Map<String, FieldDto> fields = new HashMap<String, FieldDto>();
        for (FieldDto field : type.getFields()) {
            fields.put(field.getFieldKey(), field);
        }
        for (AssetAttributeEntity attribute : attributes) {
            FieldDto field = fields.get(attribute.getAttrName());
            if (field != null && FieldKinds.USER.equals(field.getKind()) && userKey.equals(attribute.getAttrValue())) {
                return field.getLabel();
            }
        }
        return null;
    }

    private AssetEntity requireReadable(ApplicationUser user, int id) {
        AssetEntity entity = requireAsset(id);
        if (!canOpen(user, project(entity.getProjectKey()))) {
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        return entity;
    }

    private AssetEntity requireVisible(ApplicationUser user, int id) {
        AssetEntity entity = requireAsset(id);
        if (!canSee(user, project(entity.getProjectKey()))) {
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        return entity;
    }

    private AssetEntity requireWritable(ApplicationUser user, int id) {
        return requireAssetCap(user, id, kindCap(requireAsset(id)));
    }

    private String kindCap(AssetEntity entity) {
        AssetTypeEntity type = entity == null ? null : findType(entity.getTypeKey());
        return type != null && type.isLocation() ? GrantCaps.PLACES : GrantCaps.OBJECT;
    }

    private AssetEntity requireAsset(int id) {
        AssetEntity entity = ao().get(AssetEntity.class, id);
        if (entity == null) {
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        return entity;
    }

    private Project requireReadableProject(ApplicationUser user, String projectKey) {
        requireUser(user);
        if (projectKey == null || projectKey.trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.project.required");
        }
        Project project = project(projectKey.trim());
        if (project == null) {
            throw new AssetException(404, "asset-tree.error.project.notFound");
        }
        if (!canOpen(user, project)) {
            throw new AssetException(403, "asset-tree.error.project.forbidden");
        }
        return project;
    }

    private Project requireVisibleProject(ApplicationUser user, String projectKey) {
        requireUser(user);
        if (projectKey == null || projectKey.trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.project.required");
        }
        Project project = project(projectKey.trim());
        if (project == null) {
            throw new AssetException(404, "asset-tree.error.project.notFound");
        }
        if (!canSee(user, project)) {
            throw new AssetException(403, "asset-tree.error.project.forbidden");
        }
        return project;
    }

    private Project requireArrangeProject(ApplicationUser user, String projectKey) {
        requireUser(user);
        if (projectKey == null || projectKey.trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.project.required");
        }
        Project project = project(projectKey.trim());
        if (project == null) {
            throw new AssetException(404, "asset-tree.error.project.notFound");
        }
        if (!canArrangeTypes(user, project)) {
            if (!canOpen(user, project)) {
                throw new AssetException(403, "asset-tree.error.project.forbidden");
            }
            throw new AssetException(403, "asset-tree.error.forbidden");
        }
        return project;
    }

    private Project requireConfigurableProject(ApplicationUser user, String projectKey) {
        requireUser(user);
        if (projectKey == null || projectKey.trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.project.required");
        }
        Project project = project(projectKey.trim());
        if (project == null) {
            throw new AssetException(404, "asset-tree.error.project.notFound");
        }
        if (!canConfigure(user, project)) {
            if (!canOpen(user, project)) {
                throw new AssetException(403, "asset-tree.error.project.forbidden");
            }
            throw new AssetException(403, "asset-tree.error.forbidden");
        }
        return project;
    }

    private Project project(String projectKey) {
        if (projectKey == null || projectKey.trim().isEmpty()) {
            return null;
        }
        Project current = projectManager().getProjectByCurrentKey(projectKey.trim());
        return current != null ? current : projectManager().getProjectObjByKey(projectKey.trim());
    }

    private List<ProjectDto> visibleProjects(ApplicationUser user) {
        Map<String, Project> found = new LinkedHashMap<String, Project>();
        if (isAdmin(user)) {
            for (Project project : projectManager().getProjects()) {
                found.put(project.getKey(), project);
            }
        } else {
            collect(found, permissionManager().getProjects(ProjectPermissions.BROWSE_PROJECTS, user));
            collect(found, permissionManager().getProjects(ProjectPermissions.ADMINISTER_PROJECTS, user));
            includeGranted(user, found);
        }
        List<ProjectDto> projects = new ArrayList<ProjectDto>();
        for (Project project : found.values()) {
            ProjectDto dto = new ProjectDto();
            dto.setKey(project.getKey());
            dto.setName(project.getName());
            applyRights(dto, user, project);
            projects.add(dto);
        }
        Collections.sort(projects, new Comparator<ProjectDto>() {
            @Override
            public int compare(ProjectDto left, ProjectDto right) {
                return left.getName().compareToIgnoreCase(right.getName());
            }
        });
        return projects;
    }

    private void collect(Map<String, Project> found, Collection<Project> projects) {
        if (projects == null) {
            return;
        }
        for (Project project : projects) {
            if (project != null && project.getKey() != null) {
                found.put(project.getKey(), project);
            }
        }
    }

    private boolean canOpen(ApplicationUser user, Project project) {
        if (user == null || project == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return permissionManager().hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user)
                || permissionManager().hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, user)
                || grantHas(user, project, GrantCaps.VIEW);
    }

    private boolean canSee(ApplicationUser user, Project project) {
        return canOpen(user, project)
                || (user != null && project != null
                && permissionManager().hasPermission(ProjectPermissions.CREATE_ISSUES, project, user));
    }

    private boolean canWrite(ApplicationUser user, Project project) {
        return hasCap(user, project, GrantCaps.OBJECT);
    }

    private boolean canConfigure(ApplicationUser user, Project project) {
        return hasCap(user, project, GrantCaps.TYPES);
    }

    private void applyRights(ProjectDto dto, ApplicationUser user, Project project) {
        boolean places = hasCap(user, project, GrantCaps.PLACES);
        boolean objects = hasCap(user, project, GrantCaps.OBJECT);
        boolean types = hasCap(user, project, GrantCaps.TYPES);
        boolean assets = hasCap(user, project, GrantCaps.ASSETS);
        boolean admin = hasCap(user, project, GrantCaps.ADMIN);
        dto.setCanPlaces(places);
        dto.setCanObjects(objects);
        dto.setCanTypes(types);
        dto.setCanAssets(assets);
        dto.setCanAdmin(admin);
        dto.setCanEdit(objects);
        dto.setCanChange(objects);
        dto.setCanCreate(objects);
        dto.setCanMove(places || objects);
        dto.setCanRemove(places || objects);
        dto.setCanComment(places || objects);
        dto.setCanConfigure(types);
        dto.setCanGrant(assets || admin);
        dto.setCanArrange(canArrangeTypes(user, project));
    }

    private boolean canArrangeTypes(ApplicationUser user, Project project) {
        if (user == null || project == null) {
            return false;
        }
        if (isAdmin(user) || hasGlobalManage(user)) {
            return true;
        }
        if (permissionManager().hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, user)) {
            return true;
        }
        return grantHas(user, project, GrantCaps.ADMIN);
    }

    private boolean hasCap(ApplicationUser user, Project project, String cap) {
        if (user == null || project == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        if (grantHas(user, project, cap)) {
            return true;
        }
        if (GrantCaps.ADMIN.equals(cap)) {
            return false;
        }
        if (permissionManager().hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, user)) {
            return true;
        }
        if (canOpen(user, project) && (hasGlobalManage(user)
                || permissionManager().hasPermission(EDIT_ASSETS, project, user))) {
            return true;
        }
        return false;
    }

    private Project requireProjectCap(ApplicationUser user, String projectKey, String cap) {
        requireUser(user);
        if (projectKey == null || projectKey.trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.project.required");
        }
        Project project = project(projectKey.trim());
        if (project == null) {
            throw new AssetException(404, "asset-tree.error.project.notFound");
        }
        if (!hasCap(user, project, cap)) {
            if (!canOpen(user, project)) {
                throw new AssetException(403, "asset-tree.error.project.forbidden");
            }
            throw new AssetException(403, "asset-tree.error.forbidden");
        }
        return project;
    }

    private AssetEntity requireAssetCap(ApplicationUser user, int id, String cap) {
        AssetEntity entity = requireAsset(id);
        Project project = project(entity.getProjectKey());
        if (!hasCap(user, project, cap)) {
            if (!canOpen(user, project)) {
                throw new AssetException(404, "asset-tree.error.notFound");
            }
            throw new AssetException(403, "asset-tree.error.forbidden");
        }
        return entity;
    }

    private boolean canSearchGroups(ApplicationUser user) {
        if (isAdmin(user) || hasGlobalManage(user)) {
            return true;
        }
        for (ProjectDto project : visibleProjects(user)) {
            if (project.isCanGrant()) {
                return true;
            }
        }
        return false;
    }

    private boolean canEditIssue(ApplicationUser user, Issue issue) {
        return issue != null && permissionManager().hasPermission(ProjectPermissions.EDIT_ISSUES, issue, user);
    }

    private boolean canWorkIssue(ApplicationUser user, Issue issue) {
        if (issue == null || issue.getProjectObject() == null) {
            return false;
        }
        return canEditIssue(user, issue) || canWrite(user, issue.getProjectObject());
    }

    private boolean canBrowse(ApplicationUser user, Issue issue) {
        return issue != null && permissionManager().hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user);
    }

    private void requireUser(ApplicationUser user) {
        if (user == null) {
            throw new AssetException(401, "asset-tree.error.auth");
        }
    }

    private boolean isAdmin(ApplicationUser user) {
        return user != null && (globalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER, user)
                || globalPermissionManager().hasPermission(GlobalPermissionKey.SYSTEM_ADMIN, user));
    }

    private boolean hasGlobalManage(ApplicationUser user) {
        return globalPermissionManager().hasPermission(MANAGE_ASSETS, user)
                || globalPermissionManager().hasPermission(MANAGE_ASSETS_FULL, user);
    }

    private Issue requireIssue(ApplicationUser user, String issueKey) {
        String normalized = AssetValidator.normalizeIssueKey(issueKey);
        if (!AssetValidator.isIssueKey(normalized)) {
            throw new AssetException(404, "asset-tree.error.issue.notFound");
        }
        Issue issue = issueManager().getIssueObject(normalized);
        if (!canBrowse(user, issue)) {
            throw new AssetException(404, "asset-tree.error.issue.notFound");
        }
        return issue;
    }

    private AssetEntity[] assetsIn(String projectKey) {
        return ao().find(AssetEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
    }

    private AssetTypeEntity requireType(String typeKey) {
        AssetTypeEntity type = findType(typeKey);
        if (type == null) {
            throw new AssetException(400, "asset-tree.error.type");
        }
        return type;
    }

    private String parentTypeKey(String projectKey, String ownKey, String requested, boolean location) {
        String parent = requested == null ? "" : requested.trim();
        if (parent.isEmpty()) {
            return "";
        }
        if (location || (ownKey != null && ownKey.equals(parent))) {
            throw new AssetException(400, "asset-tree.error.type.parent");
        }
        AssetTypeEntity parentType = findType(parent);
        if (parentType == null || parentType.isLocation() || !ProjectKeys.same(parentType.getProjectKey(), projectKey)) {
            throw new AssetException(400, "asset-tree.error.type.parent");
        }
        String grandparent = parentType.getParentTypeKey();
        if (grandparent != null && !grandparent.trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.type.parent");
        }
        if (ownKey != null && hasChildTypes(ownKey)) {
            throw new AssetException(400, "asset-tree.error.type.parent");
        }
        return parentType.getTypeKey();
    }

    private boolean hasChildTypes(String typeKey) {
        AssetTypeEntity[] children = ao().find(AssetTypeEntity.class, Query.select().where("PARENT_TYPE_KEY = ?", typeKey));
        return children != null && children.length > 0;
    }

    private AssetTypeEntity findType(String typeKey) {
        if (typeKey == null || typeKey.trim().isEmpty()) {
            return null;
        }
        AssetTypeEntity[] found = ao().find(AssetTypeEntity.class, Query.select().where("TYPE_KEY = ?", typeKey.trim()));
        return found.length == 0 ? null : found[0];
    }

    private AssetFieldEntity[] fields(String typeKey) {
        return ao().find(AssetFieldEntity.class, Query.select().where("TYPE_KEY = ?", typeKey).order("POSITION ASC"));
    }

    private AssetFieldEntity findField(String typeKey, String fieldKey) {
        AssetFieldEntity[] found = ao().find(AssetFieldEntity.class,
                Query.select().where("TYPE_KEY = ? AND FIELD_KEY = ?", typeKey, fieldKey));
        return found.length == 0 ? null : found[0];
    }

    private Map<Integer, List<AssetAttributeEntity>> attributesByAsset(Collection<Integer> assetIds) {
        Map<Integer, List<AssetAttributeEntity>> map = new HashMap<Integer, List<AssetAttributeEntity>>();
        for (AssetAttributeEntity attribute : findIn(AssetAttributeEntity.class, "ASSET_ID", assetIds, "ATTR_POSITION ASC")) {
            Integer assetId = Integer.valueOf(attribute.getAssetId());
            List<AssetAttributeEntity> bucket = map.get(assetId);
            if (bucket == null) {
                bucket = new ArrayList<AssetAttributeEntity>();
                map.put(assetId, bucket);
            }
            bucket.add(attribute);
        }
        return map;
    }

    private List<AssetAttributeEntity> attributesFor(int assetId) {
        AssetAttributeEntity[] rows = ao().find(AssetAttributeEntity.class,
                Query.select().where("ASSET_ID = ?", assetId).order("ATTR_POSITION ASC"));
        List<AssetAttributeEntity> list = new ArrayList<AssetAttributeEntity>();
        if (rows != null) {
            Collections.addAll(list, rows);
        }
        return list;
    }

    private static List<Integer> idsOf(AssetEntity[] rows) {
        List<Integer> ids = new ArrayList<Integer>();
        if (rows == null) {
            return ids;
        }
        for (AssetEntity row : rows) {
            ids.add(Integer.valueOf(row.getID()));
        }
        return ids;
    }

    private Map<Integer, InventoryMarkEntity> marksFor(Collection<Integer> assetIds) {
        Map<Integer, InventoryMarkEntity> marks = new HashMap<Integer, InventoryMarkEntity>();
        for (InventoryMarkEntity mark : findIn(InventoryMarkEntity.class, "ASSET_ID", assetIds, null)) {
            Integer assetId = Integer.valueOf(mark.getAssetId());
            InventoryMarkEntity previous = marks.get(assetId);
            if (previous == null || newerMark(mark, previous)) {
                marks.put(assetId, mark);
            }
        }
        return marks;
    }

    private static boolean newerMark(InventoryMarkEntity candidate, InventoryMarkEntity current) {
        Date left = candidate.getCheckedAt();
        Date right = current.getCheckedAt();
        if (left == null) {
            return false;
        }
        if (right == null) {
            return true;
        }
        return left.after(right);
    }

    private <T extends RawEntity<Integer>> List<T> findIn(Class<T> type, String column, Collection<?> values, String order) {
        List<T> found = new ArrayList<T>();
        if (values == null || values.isEmpty()) {
            return found;
        }
        List<Object> items = new ArrayList<Object>();
        for (Object value : values) {
            if (value != null) {
                items.add(value);
            }
        }
        for (int offset = 0; offset < items.size(); offset += ID_BATCH) {
            int end = Math.min(offset + ID_BATCH, items.size());
            List<Object> slice = items.subList(offset, end);
            StringBuilder clause = new StringBuilder(column.length() + slice.size() * 2 + 8);
            clause.append(column).append(" IN (");
            Object[] params = new Object[slice.size()];
            for (int i = 0; i < slice.size(); i++) {
                if (i > 0) {
                    clause.append(',');
                }
                clause.append('?');
                params[i] = slice.get(i);
            }
            clause.append(')');
            Query query = Query.select().where(clause.toString(), params);
            if (order != null && !order.isEmpty()) {
                query = query.order(order);
            }
            T[] rows = ao().find(type, query);
            if (rows != null) {
                Collections.addAll(found, rows);
            }
        }
        return found;
    }

    private <T extends RawEntity<Integer>> void deleteRows(Class<T> type, String column, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (int offset = 0; offset < ids.size(); offset += ID_BATCH) {
            int end = Math.min(offset + ID_BATCH, ids.size());
            List<Integer> slice = ids.subList(offset, end);
            StringBuilder clause = new StringBuilder(column.length() + slice.size() * 2 + 8);
            clause.append(column).append(" IN (");
            Object[] params = new Object[slice.size()];
            for (int i = 0; i < slice.size(); i++) {
                if (i > 0) {
                    clause.append(',');
                }
                clause.append('?');
                params[i] = slice.get(i);
            }
            clause.append(')');
            ao().deleteWithSQL(type, clause.toString(), params);
        }
    }

    private boolean matches(AssetEntity asset, List<AssetAttributeEntity> attributes, String needle, Map<String, String[]> holders) {
        if (contains(asset.getName(), needle) || contains(asset.getObjectKey(), needle)
                || contains(asset.getDescription(), needle) || contains(asset.getCustodianKey(), needle)) {
            return true;
        }
        if (holderMatches(asset.getCustodianKey(), needle, holders)) {
            return true;
        }
        if (attributes == null) {
            return false;
        }
        for (AssetAttributeEntity attribute : attributes) {
            if (contains(attribute.getAttrName(), needle) || contains(attribute.getAttrValue(), needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean holderMatches(String userKey, String needle, Map<String, String[]> holders) {
        if (userKey == null || userKey.isEmpty()) {
            return false;
        }
        String[] cached = holders.get(userKey);
        if (cached == null) {
            ApplicationUser person = userManager().getUserByKey(userKey);
            String name = person == null || person.getDisplayName() == null ? "" : person.getDisplayName();
            String email = person == null || person.getEmailAddress() == null ? "" : person.getEmailAddress();
            cached = new String[] {name, email};
            holders.put(userKey, cached);
        }
        return contains(cached[0], needle) || contains(cached[1], needle);
    }

    private List<AssetTypeDto> typeDtos(String projectKey, AssetEntity[] assets) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        if (assets != null) {
            for (AssetEntity asset : assets) {
                Integer count = counts.get(asset.getTypeKey());
                counts.put(asset.getTypeKey(), Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            }
        }
        List<AssetTypeEntity> ordered = new ArrayList<AssetTypeEntity>();
        AssetTypeEntity[] found = ao().find(AssetTypeEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        if (found != null) {
            for (AssetTypeEntity type : found) {
                if (ProjectKeys.same(type.getProjectKey(), projectKey)) {
                    ordered.add(type);
                }
            }
        }
        Collections.sort(ordered, new Comparator<AssetTypeEntity>() {
            @Override
            public int compare(AssetTypeEntity left, AssetTypeEntity right) {
                int byOrder = Integer.compare(left.getSortOrder(), right.getSortOrder());
                return byOrder != 0 ? byOrder : left.getTypeKey().compareTo(right.getTypeKey());
            }
        });
        List<String> typeKeys = new ArrayList<String>();
        for (AssetTypeEntity type : ordered) {
            typeKeys.add(type.getTypeKey());
        }
        Map<String, List<AssetFieldEntity>> fieldsByType = fieldsByType(typeKeys);
        I18nHelper labels = i18n();
        List<AssetTypeDto> result = new ArrayList<AssetTypeDto>();
        for (AssetTypeEntity type : ordered) {
            Integer count = counts.get(type.getTypeKey());
            List<AssetFieldEntity> typeFields = fieldsByType.get(type.getTypeKey());
            result.add(toTypeDto(type, labels, count == null ? 0 : count.intValue(),
                    typeFields == null ? Collections.<AssetFieldEntity>emptyList() : typeFields));
        }
        return result;
    }

    private Map<String, List<AssetFieldEntity>> fieldsByType(List<String> typeKeys) {
        Map<String, List<AssetFieldEntity>> map = new HashMap<String, List<AssetFieldEntity>>();
        for (AssetFieldEntity field : findIn(AssetFieldEntity.class, "TYPE_KEY", typeKeys, "POSITION ASC")) {
            List<AssetFieldEntity> bucket = map.get(field.getTypeKey());
            if (bucket == null) {
                bucket = new ArrayList<AssetFieldEntity>();
                map.put(field.getTypeKey(), bucket);
            }
            bucket.add(field);
        }
        return map;
    }

    private AssetTypeDto toTypeDto(AssetTypeEntity type, I18nHelper labels, int assetCount) {
        AssetFieldEntity[] stored = fields(type.getTypeKey());
        List<AssetFieldEntity> typeFields = new ArrayList<AssetFieldEntity>();
        if (stored != null) {
            Collections.addAll(typeFields, stored);
        }
        return toTypeDto(type, labels, assetCount, typeFields);
    }

    private AssetTypeDto toTypeDto(AssetTypeEntity type, I18nHelper labels, int assetCount, List<AssetFieldEntity> typeFields) {
        AssetTypeDto dto = new AssetTypeDto();
        dto.setTypeKey(type.getTypeKey());
        dto.setProjectKey(type.getProjectKey());
        dto.setColor(type.getColor());
        dto.setIcon(TypeIcons.resolve(type.getIcon(), type.isLocation()));
        dto.setSystemType(type.isSystemType());
        dto.setLocation(type.isLocation());
        dto.setShowInTree(type.isLocation() || type.isShowInTree());
        dto.setService(!type.isLocation() && type.isService());
        dto.setPlaceCaption(type.getPlaceCaption() == null ? "" : type.getPlaceCaption());
        dto.setParentTypeKey(type.getParentTypeKey() == null ? "" : type.getParentTypeKey());
        dto.setAssetCount(assetCount);
        if (type.isSystemType() && type.getBaseKey() != null && !type.getBaseKey().isEmpty()) {
            String key = "asset-tree.type." + type.getBaseKey();
            String label = labels.getText(key);
            dto.setLabel(key.equals(label) ? type.getLabel() : label);
        } else {
            dto.setLabel(type.getLabel());
        }
        List<FieldDto> fields = new ArrayList<FieldDto>();
        if (typeFields != null) {
            for (AssetFieldEntity field : typeFields) {
                fields.add(toFieldDto(field, labels));
            }
        }
        dto.setFields(fields);
        return dto;
    }

    private FieldDto toFieldDto(AssetFieldEntity field, I18nHelper labels) {
        FieldDto dto = new FieldDto();
        dto.setFieldKey(field.getFieldKey());
        dto.setKind(field.getKind());
        dto.setRequired(field.isRequired());
        dto.setShowOnIssue(!field.isIssueHidden());
        dto.setPosition(field.getPosition());
        dto.setLabel(fieldLabel(field, labels));
        dto.setOptions(FieldChoices.optionsOf(field.getOptions()));
        return dto;
    }

    private String resolveIcon(String raw, boolean location) {
        String icon = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (icon.isEmpty()) {
            return TypeIcons.defaultFor(location);
        }
        if (!TypeIcons.isIcon(icon)) {
            throw new AssetException(400, "asset-tree.error.type.icon");
        }
        return icon;
    }

    private boolean hidesOnIssue(FieldDraft draft) {
        return draft.getShowOnIssue() != null && !draft.getShowOnIssue().booleanValue();
    }

    private String fieldLabel(AssetFieldEntity field, I18nHelper labels) {
        String stored = field.getLabel();
        if (stored != null && stored.startsWith("asset-tree.")) {
            String translated = labels.getText(stored);
            if (!stored.equals(translated)) {
                return translated;
            }
        }
        return stored;
    }

    private String resolveStatus(String projectKey, String raw) {
        ensureStatuses(projectKey);
        String key = Statuses.canonical(raw);
        if (findStatus(projectKey, key) == null) {
            throw new AssetException(400, "asset-tree.error.status");
        }
        return key;
    }

    private void ensureStatuses(String projectKey) {
        if (projectStatuses(projectKey).length > 0) {
            return;
        }
        String[] keys = new String[] {
                Statuses.IN_STOCK, Statuses.IN_USE, Statuses.REPAIR,
                Statuses.RESERVE, Statuses.MAINTENANCE, Statuses.WRITTEN_OFF
        };
        String[] labelKeys = new String[] {
                "asset-tree.ui.statusStock", "asset-tree.ui.statusInUse", "asset-tree.ui.statusRepair",
                "asset-tree.ui.statusReserve", "asset-tree.ui.statusMaintenance", "asset-tree.ui.statusWrittenOff"
        };
        String[] categories = new String[] {"todo", "progress", "progress", "todo", "progress", "done"};
        I18nHelper labels = i18n();
        for (int i = 0; i < keys.length; i++) {
            String label = labels.getText(labelKeys[i]);
            if (label == null || label.equals(labelKeys[i])) {
                label = keys[i];
            }
            ao().create(ProjectStatusEntity.class,
                    new DBParam("PROJECT_KEY", projectKey),
                    new DBParam("STATUS_KEY", keys[i]),
                    new DBParam("LABEL", label),
                    new DBParam("CATEGORY", categories[i]),
                    new DBParam("SORT_ORDER", i),
                    new DBParam("SUMMARY_MODE", StatusSummary.seedMode(keys[i])));
        }
    }

    private ProjectStatusEntity[] projectStatuses(String projectKey) {
        return ao().find(ProjectStatusEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey).order("SORT_ORDER ASC"));
    }

    private ProjectStatusEntity findStatus(String projectKey, String statusKey) {
        if (projectKey == null || statusKey == null || statusKey.trim().isEmpty()) {
            return null;
        }
        ProjectStatusEntity[] found = ao().find(ProjectStatusEntity.class,
                Query.select().where("PROJECT_KEY = ? AND STATUS_KEY = ?", projectKey, statusKey.trim()));
        return found.length == 0 ? null : found[0];
    }

    private List<StatusDto> statusDtos(String projectKey, AssetEntity[] assets) {
        ensureStatuses(projectKey);
        Map<String, Integer> counts = new HashMap<String, Integer>();
        Set<String> places = locationTypeKeys(projectKey);
        if (assets != null) {
            for (AssetEntity asset : assets) {
                if (asset.getTypeKey() != null && places.contains(asset.getTypeKey())) {
                    continue;
                }
                String key = Statuses.canonical(asset.getStatus());
                Integer count = counts.get(key);
                counts.put(key, Integer.valueOf((count == null ? 0 : count.intValue()) + 1));
            }
        }
        List<StatusDto> result = new ArrayList<StatusDto>();
        for (ProjectStatusEntity row : projectStatuses(projectKey)) {
            result.add(toStatusDto(row, counts.get(row.getStatusKey())));
        }
        return result;
    }

    private StatusDto toStatusDto(ProjectStatusEntity row, Integer count) {
        StatusDto dto = new StatusDto();
        dto.setStatusKey(row.getStatusKey());
        dto.setLabel(row.getLabel());
        dto.setCategory(row.getCategory());
        dto.setSortOrder(row.getSortOrder());
        dto.setAssetCount(count == null ? 0 : count.intValue());
        dto.setInSummary(StatusSummary.shows(row.getSummaryMode(), row.getStatusKey()));
        return dto;
    }

    private int countStatus(String projectKey, String statusKey) {
        int count = 0;
        Set<String> places = locationTypeKeys(projectKey);
        for (AssetEntity row : assetsIn(projectKey)) {
            if (row.getTypeKey() != null && places.contains(row.getTypeKey())) {
                continue;
            }
            if (statusKey.equals(Statuses.canonical(row.getStatus()))) {
                count++;
            }
        }
        return count;
    }

    /** Places store a status, but the screen never shows it. Keep that value off the equipment counts. */
    private Set<String> locationTypeKeys(String projectKey) {
        Set<String> keys = new HashSet<String>();
        AssetTypeEntity[] found = ao().find(AssetTypeEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        if (found == null) {
            return keys;
        }
        for (AssetTypeEntity type : found) {
            if (type.isLocation() && ProjectKeys.same(type.getProjectKey(), projectKey) && type.getTypeKey() != null) {
                keys.add(type.getTypeKey());
            }
        }
        return keys;
    }

    private void releasePlaceStatus(String projectKey, String removed, String fallback) {
        Set<String> places = locationTypeKeys(projectKey);
        for (AssetEntity asset : assetsIn(projectKey)) {
            if (asset.getTypeKey() == null || !places.contains(asset.getTypeKey())) {
                continue;
            }
            if (!removed.equals(Statuses.canonical(asset.getStatus()))) {
                continue;
            }
            asset.setStatus(fallback);
            asset.save();
        }
    }

    private String normalizeCategory(String category) {
        String value = category == null ? "" : category.trim();
        if (value.isEmpty()) {
            return "todo";
        }
        if (!StatusCategories.allowed(value)) {
            throw new AssetException(400, "asset-tree.error.status");
        }
        return value;
    }

    private String categoryColor(String category) {
        return StatusCategories.color(category);
    }

    private List<GrantDto> grantDtos(String projectKey) {
        List<GrantDto> result = new ArrayList<GrantDto>();
        ProjectGrantEntity[] rows = ao().find(ProjectGrantEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        ProjectGrantEntity[] global = ao().find(ProjectGrantEntity.class, Query.select().where("PROJECT_KEY = ?", GrantCaps.ALL_PROJECTS));
        for (ProjectGrantEntity row : rows) {
            result.add(toGrantDto(row));
        }
        for (ProjectGrantEntity row : global) {
            boolean listed = false;
            for (GrantDto existing : result) {
                if (existing.getGroupName() != null && existing.getGroupName().equalsIgnoreCase(row.getGroupName())) {
                    listed = true;
                }
            }
            if (!listed) {
                result.add(toGrantDto(row));
            }
        }
        Collections.sort(result, new Comparator<GrantDto>() {
            @Override
            public int compare(GrantDto left, GrantDto right) {
                String a = left.getGroupName() == null ? "" : left.getGroupName();
                String b = right.getGroupName() == null ? "" : right.getGroupName();
                return a.compareToIgnoreCase(b);
            }
        });
        return result;
    }

    private GrantDto toGrantDto(ProjectGrantEntity row) {
        GrantDto dto = new GrantDto();
        dto.setGroupName(row.getGroupName());
        String caps = row.getCaps();
        if (caps == null || caps.trim().isEmpty()) {
            caps = GrantCaps.fromLevel(row.getLevel());
        } else {
            caps = GrantCaps.normalize(caps);
        }
        dto.setCaps(caps);
        dto.setLevel(GrantCaps.levelOf(caps));
        return dto;
    }

    private String resolveCaps(GrantDraft draft) {
        String caps = draft.getCaps() == null ? "" : GrantCaps.normalize(draft.getCaps());
        if (caps.isEmpty()) {
            caps = GrantCaps.fromLevel(draft.getLevel());
        }
        if (caps.isEmpty()) {
            throw new AssetException(400, "asset-tree.error.group");
        }
        return caps;
    }

    private ProjectGrantEntity findGrant(String projectKey, String groupName) {
        ProjectGrantEntity[] found = ao().find(ProjectGrantEntity.class,
                Query.select().where("PROJECT_KEY = ? AND GROUP_NAME = ?", projectKey, groupName));
        return found.length == 0 ? null : found[0];
    }

    private boolean groupExists(String groupName) {
        try {
            return groupManager().getGroup(groupName) != null;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private void includeGranted(ApplicationUser user, Map<String, Project> found) {
        if (user == null) {
            return;
        }
        try {
            ProjectGrantEntity[] grants = ao().find(ProjectGrantEntity.class);
            for (ProjectGrantEntity grant : grants) {
                if (grant.getProjectKey() == null || !inGroup(user, grant.getGroupName())) {
                    continue;
                }
                if (GrantCaps.ALL_PROJECTS.equals(grant.getProjectKey())) {
                    String caps = grant.getCaps();
                    if (caps == null || caps.trim().isEmpty()) {
                        caps = GrantCaps.fromLevel(grant.getLevel());
                    }
                    if (GrantCaps.has(caps, GrantCaps.ADMIN)) {
                        for (Project granted : projectManager().getProjects()) {
                            if (granted != null && granted.getKey() != null) {
                                found.put(granted.getKey(), granted);
                            }
                        }
                    }
                    continue;
                }
                if (found.containsKey(grant.getProjectKey())) {
                    continue;
                }
                Project granted = project(grant.getProjectKey());
                if (granted != null) {
                    found.put(granted.getKey(), granted);
                }
            }
        } catch (RuntimeException ex) {
            log.debug("Grant project lookup skipped", ex);
        }
    }

    private boolean grantHas(ApplicationUser user, Project project, String cap) {
        if (user == null || project == null || project.getKey() == null) {
            return false;
        }
        try {
            if (grantRowsAllow(user, project.getKey(), cap)) {
                return true;
            }
            return grantRowsAllow(user, GrantCaps.ALL_PROJECTS, cap);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean grantRowsAllow(ApplicationUser user, String projectKey, String cap) {
        ProjectGrantEntity[] rows = ao().find(ProjectGrantEntity.class,
                Query.select().where("PROJECT_KEY = ?", projectKey));
        for (int i = 0; i < rows.length; i++) {
            ProjectGrantEntity row = rows[i];
            if (!inGroup(user, row.getGroupName())) {
                continue;
            }
            String caps = row.getCaps();
            if (caps == null || caps.trim().isEmpty()) {
                caps = GrantCaps.fromLevel(row.getLevel());
            }
            if (GrantCaps.has(caps, cap)) {
                return true;
            }
        }
        return false;
    }

    private boolean inGroup(ApplicationUser user, String groupName) {
        if (user == null || groupName == null || groupName.trim().isEmpty()) {
            return false;
        }
        try {
            return groupManager().isUserInGroup(user, groupName.trim());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String statusText(String status) {
        String key = "asset-tree.ui.statusInUse";
        if (Statuses.IN_STOCK.equals(status)) {
            key = "asset-tree.ui.statusStock";
        } else if (Statuses.REPAIR.equals(status)) {
            key = "asset-tree.ui.statusRepair";
        } else if (Statuses.RESERVE.equals(status)) {
            key = "asset-tree.ui.statusReserve";
        } else if (Statuses.MAINTENANCE.equals(status)) {
            key = "asset-tree.ui.statusMaintenance";
        } else if (Statuses.WRITTEN_OFF.equals(status)) {
            key = "asset-tree.ui.statusWrittenOff";
        }
        return i18n().getText(key);
    }

    private Map<String, AssetTypeDto> indexTypes(List<AssetTypeDto> types) {
        Map<String, AssetTypeDto> index = new HashMap<String, AssetTypeDto>();
        for (AssetTypeDto type : types) {
            index.put(type.getTypeKey(), type);
        }
        return index;
    }

    private Map<Integer, AssetEntity> indexAssets(AssetEntity[] assets) {
        Map<Integer, AssetEntity> index = new HashMap<Integer, AssetEntity>();
        if (assets != null) {
            for (AssetEntity asset : assets) {
                index.put(asset.getID(), asset);
            }
        }
        return index;
    }

    private AssetDto toDto(AssetEntity entity, List<AssetAttributeEntity> attributes, Map<String, AssetTypeDto> types,
                           Map<Integer, AssetEntity> index, boolean withIssues) {
        AssetDto dto = new AssetDto();
        dto.setId(entity.getID());
        dto.setObjectKey(entity.getObjectKey());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        dto.setTypeKey(entity.getTypeKey());
        dto.setStatus(Statuses.canonical(entity.getStatus()));
        dto.setParentId(TreeLogic.normalizeParent(entity.getParentId()));
        dto.setSortOrder(entity.getSortOrder());
        dto.setCreated(format(entity.getCreated()));
        dto.setUpdated(format(entity.getUpdated()));
        dto.setCreatedBy(displayName(entity.getCreatedBy()));
        dto.setUpdatedBy(displayName(entity.getUpdatedBy()));
        dto.setProjectKey(entity.getProjectKey());
        Project project = project(entity.getProjectKey());
        dto.setProjectName(project == null ? entity.getProjectKey() : project.getName());
        dto.setEditable(canWrite(authenticationContext().getLoggedInUser(), project));
        String cardImage = entity.getCardImage();
        dto.setCardImage(cardImage != null && !cardImage.isEmpty());
        dto.setLocation(location(entity, index));
        dto.setCustodian(profile(entity.getCustodianKey()));
        AssetTypeDto type = types.get(entity.getTypeKey());
        if (type != null) {
            dto.setTypeLabel(type.getLabel());
            dto.setColor(type.getColor());
            dto.setIcon(type.getIcon());
        } else {
            dto.setTypeLabel(entity.getTypeKey());
            dto.setColor("#5D6B82");
            dto.setIcon(TypeIcons.DEFAULT_OBJECT);
        }
        dto.setAttributes(attributeDtos(type, attributes));
        List<ServicePlanEntity> plans = plansOf(entity.getID());
        dto.setServiceDue(dueNames(plans, LocalDate.now()));
        dto.setOfferedTypes(type != null && type.isLocation() ? offeredLines(entity.getOfferedTypes()) : new ArrayList<String>());
        if (withIssues) {
            dto.setIssues(issuesFor(entity.getID()));
            dto.setComments(commentDtos(entity.getID()));
            dto.setFiles(fileDtos(entity.getID()));
            dto.setActivities(activityDtos(entity.getID()));
            dto.setPlans(planDtos(plans, LocalDate.now()));
        }
        return dto;
    }

    private void logActivity(int assetId, String authorKey, String kind, String field, String oldValue, String newValue) {
        String left = oldValue == null ? "" : oldValue;
        String right = newValue == null ? "" : newValue;
        if (left.equals(right)) {
            return;
        }
        recordActivity(assetId, authorKey, kind, field, left, right);
    }

    private void recordActivity(int assetId, String authorKey, String kind, String field, String oldValue, String newValue) {
        ao().create(AssetActivityEntity.class,
                new DBParam("ASSET_ID", assetId),
                new DBParam("AUTHOR_KEY", authorKey == null ? "" : authorKey),
                new DBParam("KIND", kind),
                new DBParam("FIELD_NAME", field == null ? "" : field),
                new DBParam("OLD_VALUE", clip(oldValue)),
                new DBParam("NEW_VALUE", clip(newValue)),
                new DBParam("CREATED", new Date()));
    }

    private String clip(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private Map<String, String> attributeValues(int assetId) {
        Map<String, String> values = new HashMap<String, String>();
        for (AssetAttributeEntity attribute : attributesFor(assetId)) {
            values.put(attribute.getAttrName(), attribute.getAttrValue() == null ? "" : attribute.getAttrValue().trim());
        }
        return values;
    }

    private void logAttributeChanges(int assetId, String authorKey, String typeKey, Map<String, String> before) {
        Map<String, String> after = attributeValues(assetId);
        Map<String, String> names = new HashMap<String, String>();
        I18nHelper labels = i18n();
        for (AssetFieldEntity field : fields(typeKey)) {
            names.put(field.getFieldKey(), fieldLabel(field, labels));
        }
        Set<String> keys = new HashSet<String>();
        keys.addAll(before.keySet());
        keys.addAll(after.keySet());
        for (String key : keys) {
            String oldValue = before.containsKey(key) ? before.get(key) : "";
            String newValue = after.containsKey(key) ? after.get(key) : "";
            String label = names.containsKey(key) ? names.get(key) : key;
            logActivity(assetId, authorKey, "attribute", label, oldValue, newValue);
        }
    }

    private String typeLabel(AssetTypeEntity type) {
        if (type == null) {
            return "";
        }
        if (type.isSystemType() && type.getBaseKey() != null && !type.getBaseKey().isEmpty()) {
            String key = "asset-tree.type." + type.getBaseKey();
            String label = i18n().getText(key);
            return key.equals(label) ? type.getLabel() : label;
        }
        return type.getLabel() == null ? "" : type.getLabel();
    }

    private String parentName(Integer parentId) {
        if (parentId == null) {
            return "";
        }
        AssetEntity parent = ao().get(AssetEntity.class, parentId.intValue());
        return parent == null || parent.getName() == null ? "" : parent.getName();
    }

    private List<ActivityDto> activityDtos(int assetId) {
        AssetActivityEntity[] rows = ao().find(AssetActivityEntity.class, Query.select().where("ASSET_ID = ?", assetId));
        List<ActivityDto> result = new ArrayList<ActivityDto>();
        if (rows == null) {
            return result;
        }
        List<AssetActivityEntity> ordered = new ArrayList<AssetActivityEntity>();
        Collections.addAll(ordered, rows);
        Collections.sort(ordered, new Comparator<AssetActivityEntity>() {
            @Override
            public int compare(AssetActivityEntity left, AssetActivityEntity right) {
                Date leftCreated = left.getCreated();
                Date rightCreated = right.getCreated();
                if (leftCreated == null || rightCreated == null) {
                    return left.getID() - right.getID();
                }
                int byTime = leftCreated.compareTo(rightCreated);
                return byTime == 0 ? left.getID() - right.getID() : byTime;
            }
        });
        for (AssetActivityEntity row : ordered) {
            ActivityDto dto = new ActivityDto();
            dto.setId(row.getID());
            dto.setAction(row.getKind() == null ? "" : row.getKind());
            dto.setField(row.getFieldName() == null ? "" : row.getFieldName());
            dto.setOldValue(row.getOldValue() == null ? "" : row.getOldValue());
            dto.setNewValue(row.getNewValue() == null ? "" : row.getNewValue());
            dto.setCreated(format(row.getCreated()));
            if (row.getAuthorKey() == null || row.getAuthorKey().isEmpty()) {
                UserProfileDto actor = new UserProfileDto();
                actor.setDisplayName(mailText("asset-tree.ui.serviceActor"));
                dto.setAuthor(actor);
            } else {
                dto.setAuthor(profile(row.getAuthorKey()));
            }
            result.add(dto);
        }
        return result;
    }

    private List<CommentDto> commentDtos(int assetId) {
        AssetCommentEntity[] rows = ao().find(AssetCommentEntity.class, Query.select().where("ASSET_ID = ?", assetId));
        List<CommentDto> result = new ArrayList<CommentDto>();
        if (rows == null) {
            return result;
        }
        List<AssetCommentEntity> ordered = new ArrayList<AssetCommentEntity>();
        for (AssetCommentEntity row : rows) {
            ordered.add(row);
        }
        Collections.sort(ordered, new Comparator<AssetCommentEntity>() {
            @Override
            public int compare(AssetCommentEntity left, AssetCommentEntity right) {
                Date leftCreated = left.getCreated();
                Date rightCreated = right.getCreated();
                if (leftCreated == null || rightCreated == null) {
                    return left.getID() - right.getID();
                }
                int byTime = leftCreated.compareTo(rightCreated);
                return byTime == 0 ? left.getID() - right.getID() : byTime;
            }
        });
        for (AssetCommentEntity row : ordered) {
            result.add(commentDto(row));
        }
        return result;
    }

    private CommentDto commentDto(AssetCommentEntity row) {
        CommentDto dto = new CommentDto();
        dto.setId(row.getID());
        dto.setBody(row.getBody() == null ? "" : row.getBody());
        dto.setCreated(format(row.getCreated()));
        dto.setAuthor(profile(row.getAuthorKey()));
        return dto;
    }

    private List<FileDto> fileDtos(int assetId) {
        AssetFileEntity[] rows = ao().find(AssetFileEntity.class, Query.select().where("ASSET_ID = ?", assetId));
        List<FileDto> result = new ArrayList<FileDto>();
        if (rows == null) {
            return result;
        }
        for (AssetFileEntity row : rows) {
            result.add(fileDto(row));
        }
        return result;
    }

    private FileDto fileDto(AssetFileEntity row) {
        FileDto dto = new FileDto();
        dto.setId(row.getID());
        dto.setFileName(row.getFileName());
        dto.setContentType(row.getContentType());
        dto.setSize(row.getSizeBytes());
        dto.setCreated(format(row.getCreated()));
        UserProfileDto author = profile(row.getAuthorKey());
        dto.setAuthorName(author == null ? "" : author.getDisplayName());
        return dto;
    }

    private String fileNameOf(String fileName) {
        String name = fileName == null ? "" : fileName.trim();
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (name.isEmpty() || ".".equals(name) || "..".equals(name)) {
            throw new AssetException(400, "asset-tree.error.file.name");
        }
        if (name.length() > 180) {
            name = name.substring(name.length() - 180);
        }
        return name;
    }

    private List<AttributeDto> attributeDtos(AssetTypeDto type, List<AssetAttributeEntity> stored) {
        Map<String, String> values = new HashMap<String, String>();
        if (stored != null) {
            for (AssetAttributeEntity attribute : stored) {
                values.put(attribute.getAttrName(), attribute.getAttrValue() == null ? "" : attribute.getAttrValue());
            }
        }
        List<AttributeDto> result = new ArrayList<AttributeDto>();
        if (type == null || type.getFields() == null) {
            return result;
        }
        for (FieldDto field : type.getFields()) {
            AttributeDto dto = new AttributeDto();
            dto.setFieldKey(field.getFieldKey());
            dto.setName(field.getLabel());
            dto.setKind(field.getKind());
            dto.setRequired(field.isRequired());
            dto.setShowOnIssue(field.isShowOnIssue());
            String value = values.containsKey(field.getFieldKey()) ? values.get(field.getFieldKey()) : "";
            dto.setValue(value);
            dto.setDisplayValue(FieldKinds.USER.equals(field.getKind()) ? userLabel(value) : value);
            result.add(dto);
        }
        return result;
    }

    private String location(AssetEntity entity, Map<Integer, AssetEntity> index) {
        List<String> names = new ArrayList<String>();
        Integer parentId = TreeLogic.normalizeParent(entity.getParentId());
        int guard = 0;
        while (parentId != null && guard < 80) {
            AssetEntity parent = index.get(parentId);
            if (parent == null) {
                break;
            }
            names.add(0, parent.getName());
            parentId = TreeLogic.normalizeParent(parent.getParentId());
            guard++;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                builder.append(" / ");
            }
            builder.append(names.get(i));
        }
        return builder.toString();
    }

    private List<IssueRefDto> issuesFor(int assetId) {
        AssetIssueLinkEntity[] links = ao().find(AssetIssueLinkEntity.class, Query.select().where("ASSET_ID = ?", assetId));
        ApplicationUser user = authenticationContext().getLoggedInUser();
        List<IssueRefDto> issues = new ArrayList<IssueRefDto>();
        for (AssetIssueLinkEntity link : links) {
            Issue issue = issueManager().getIssueObject(Long.valueOf(link.getIssueId()));
            if (!canBrowse(user, issue)) {
                continue;
            }
            issues.add(toIssue(issue));
        }
        Collections.sort(issues, new Comparator<IssueRefDto>() {
            @Override
            public int compare(IssueRefDto left, IssueRefDto right) {
                return left.getIssueKey().compareToIgnoreCase(right.getIssueKey());
            }
        });
        return issues;
    }

    private IssueRefDto toIssue(Issue issue) {
        IssueRefDto dto = new IssueRefDto();
        dto.setIssueId(issue.getId() == null ? 0L : issue.getId().longValue());
        dto.setIssueKey(issue.getKey());
        dto.setSummary(issue.getSummary());
        if (issue.getStatus() != null) {
            dto.setStatus(issue.getStatus().getNameTranslation());
        }
        return dto;
    }

    private UserProfileDto profile(String userKey) {
        if (userKey == null || userKey.isEmpty()) {
            return null;
        }
        ApplicationUser user = userManager().getUserByKey(userKey);
        UserProfileDto dto = new UserProfileDto();
        dto.setUserKey(userKey);
        if (user == null) {
            dto.setDisplayName(userKey);
            dto.setActive(false);
            return dto;
        }
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setEmail(user.getEmailAddress());
        dto.setActive(user.isActive());
        try {
            Directory directory = crowdDirectoryService().findDirectoryById(user.getDirectoryId());
            if (directory != null) {
                dto.setDirectory(directory.getName());
            }
        } catch (RuntimeException ex) {
            log.debug("Directory lookup failed for {}", userKey, ex);
        }
        try {
            UserWithAttributes attributes = crowdService().getUserWithAttributes(user.getName());
            if (attributes != null) {
                dto.setPhone(firstValue(attributes, "telephoneNumber", "phone", "mobile", "mobilePhone"));
                dto.setDepartment(firstValue(attributes, "department", "departmentNumber"));
                dto.setTitle(firstValue(attributes, "title", "jobTitle"));
            }
        } catch (RuntimeException ex) {
            log.debug("Directory attributes failed for {}", userKey, ex);
        }
        return dto;
    }

    private String firstValue(UserWithAttributes attributes, String... keys) {
        for (String key : keys) {
            String value = attributes.getValue(key);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String userLabel(String userKey) {
        if (userKey == null || userKey.trim().isEmpty()) {
            return "";
        }
        try {
            ApplicationUser user = userManager().getUserByKey(userKey.trim());
            if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                return user.getDisplayName();
            }
        } catch (RuntimeException ex) {
            log.debug("User {} was not resolved", userKey, ex);
        }
        return userKey;
    }

    private String displayName(String userKey) {
        UserProfileDto profile = profile(userKey);
        return profile == null ? "" : profile.getDisplayName();
    }

    private String format(Date date) {
        return date == null ? null : date.toInstant().toString();
    }

    private I18nHelper i18n() {
        return authenticationContext().getI18nHelper();
    }

    private Integer parseId(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (!trimmed.matches("\\d+")) {
            return null;
        }
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<Integer, Integer> parentMap(AssetEntity[] assets) {
        Map<Integer, Integer> parents = new HashMap<Integer, Integer>();
        for (AssetEntity asset : assets) {
            parents.put(asset.getID(), TreeLogic.normalizeParent(asset.getParentId()));
        }
        return parents;
    }

    private Map<Integer, List<Integer>> childrenMap(AssetEntity[] assets) {
        Map<Integer, List<Integer>> children = new HashMap<Integer, List<Integer>>();
        for (AssetEntity asset : assets) {
            Integer parentId = TreeLogic.normalizeParent(asset.getParentId());
            if (parentId == null) {
                continue;
            }
            List<Integer> bucket = children.get(parentId);
            if (bucket == null) {
                bucket = new ArrayList<Integer>();
                children.put(parentId, bucket);
            }
            bucket.add(asset.getID());
        }
        return children;
    }

    private static final int MAX_PLANS = 12;

    @Override
    public ServicePlanDto createPlan(ApplicationUser user, final int assetId, final ServicePlanDraft draft) {
        final List<ServiceNotice> notices = new ArrayList<ServiceNotice>();
        ServicePlanDto created = ao().executeInTransaction(new TransactionCallback<ServicePlanDto>() {
            @Override
            public ServicePlanDto doInTransaction() {
                AssetEntity asset = equipmentForPlan(user, assetId);
                AssetTypeEntity type = findType(asset.getTypeKey());
                if (type == null || !type.isService()) {
                    throw new AssetException(400, "asset-tree.error.service.type");
                }
                if (!moduleOn(asset.getProjectKey(), ProjectModules.SERVICE)) {
                    throw new AssetException(400, "asset-tree.error.module.off");
                }
                if (plansOf(assetId).size() >= MAX_PLANS) {
                    throw new AssetException(400, "asset-tree.error.service.limit");
                }
                ServicePlanEntity row = ao().create(ServicePlanEntity.class, new DBParam("ASSET_ID", assetId));
                fillPlan(row, asset, draft, true);
                row.save();
                applyDueQuietly(asset.getProjectKey(), notices);
                return planDto(row, LocalDate.now());
            }
        });
        dispatchNotices(notices);
        return created;
    }

    @Override
    public ServicePlanDto updatePlan(ApplicationUser user, final int assetId, final int planId, final ServicePlanDraft draft) {
        final List<ServiceNotice> notices = new ArrayList<ServiceNotice>();
        ServicePlanDto updated = ao().executeInTransaction(new TransactionCallback<ServicePlanDto>() {
            @Override
            public ServicePlanDto doInTransaction() {
                AssetEntity asset = equipmentForPlan(user, assetId);
                ServicePlanEntity row = findPlan(assetId, planId);
                if (row == null) {
                    throw new AssetException(404, "asset-tree.error.notFound");
                }
                fillPlan(row, asset, draft, false);
                row.save();
                applyDueQuietly(asset.getProjectKey(), notices);
                return planDto(row, LocalDate.now());
            }
        });
        dispatchNotices(notices);
        return updated;
    }

    @Override
    public void deletePlan(ApplicationUser user, final int assetId, final int planId) {
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                equipmentForPlan(user, assetId);
                ServicePlanEntity row = findPlan(assetId, planId);
                if (row == null) {
                    throw new AssetException(404, "asset-tree.error.notFound");
                }
                ao().delete(row);
                return null;
            }
        });
    }

    @Override
    public AssetDto offerType(ApplicationUser user, final int assetId, final PlaceTypeDraft draft, final boolean present) {
        final List<String> requested = requestedTypes(draft);
        if (requested.isEmpty()) {
            throw new AssetException(400, "asset-tree.error.type.notFound");
        }
        return ao().executeInTransaction(new TransactionCallback<AssetDto>() {
            @Override
            public AssetDto doInTransaction() {
                AssetEntity place = requireAssetCap(user, assetId, GrantCaps.PLACES);
                if (!isLocation(place)) {
                    throw new AssetException(400, "asset-tree.error.type.place");
                }
                List<String> lines = offeredLines(place.getOfferedTypes());
                List<PlaceLayout.Fact> facts = placeFacts(place.getProjectKey());
                for (String typeKey : requested) {
                    AssetTypeEntity type = requireType(typeKey);
                    if (type.isLocation() || !ProjectKeys.same(type.getProjectKey(), place.getProjectKey())) {
                        throw new AssetException(400, "asset-tree.error.type");
                    }
                    if (present) {
                        List<String> next = PlaceLayout.addPin(lines, facts, type.getTypeKey());
                        if (next == null) {
                            throw new AssetException(400, "asset-tree.error.type");
                        }
                        lines = next;
                    } else {
                        if (placeHasEquipment(place, type.getTypeKey())) {
                            throw new AssetException(409, "asset-tree.error.type.busy");
                        }
                        lines = PlaceLayout.removePin(lines, type.getTypeKey());
                    }
                }
                place.setOfferedTypes(joinOffered(lines));
                place.setUpdated(new Date());
                place.setUpdatedBy(user.getKey());
                place.save();
                AssetEntity[] rows = assetsIn(place.getProjectKey());
                return toDto(place, attributesFor(assetId), indexTypes(typeDtos(place.getProjectKey(), rows)), indexAssets(rows), false);
            }
        });
    }

    @Override
    public AssetDto arrangeType(ApplicationUser user, final int assetId, final PlaceArrangeDraft draft) {
        if (draft == null || draft.getTypeKey() == null || draft.getTypeKey().trim().isEmpty()) {
            throw new AssetException(400, "asset-tree.error.type.arrange");
        }
        final String typeKey = draft.getTypeKey().trim();
        final String action = draft.getAction() == null ? "" : draft.getAction().trim();
        return ao().executeInTransaction(new TransactionCallback<AssetDto>() {
            @Override
            public AssetDto doInTransaction() {
                AssetEntity place = requireAsset(assetId);
                Project project = project(place.getProjectKey());
                if (!canArrangeTypes(user, project)) {
                    if (!canOpen(user, project)) {
                        throw new AssetException(404, "asset-tree.error.notFound");
                    }
                    throw new AssetException(403, "asset-tree.error.forbidden");
                }
                if (!isLocation(place)) {
                    throw new AssetException(400, "asset-tree.error.type.place");
                }
                AssetTypeEntity type = requireType(typeKey);
                if (type.isLocation() || !ProjectKeys.same(type.getProjectKey(), place.getProjectKey())) {
                    throw new AssetException(400, "asset-tree.error.type.arrange");
                }
                List<String> pinned = offeredKeys(place.getOfferedTypes());
                for (AssetEntity asset : assetsIn(place.getProjectKey())) {
                    Integer parent = TreeLogic.normalizeParent(asset.getParentId());
                    if (parent != null && parent.intValue() == place.getID() && !isLocation(asset)
                            && asset.getTypeKey() != null && !pinned.contains(asset.getTypeKey())) {
                        pinned.add(asset.getTypeKey());
                    }
                }
                List<String> next = PlaceLayout.move(offeredLines(place.getOfferedTypes()), pinned,
                        placeFacts(place.getProjectKey()), type.getTypeKey(), action);
                if (next == null) {
                    throw new AssetException(400, "asset-tree.error.type.arrange");
                }
                place.setOfferedTypes(joinOffered(next));
                place.setUpdated(new Date());
                place.setUpdatedBy(user.getKey());
                place.save();
                AssetEntity[] rows = assetsIn(place.getProjectKey());
                return toDto(place, attributesFor(assetId), indexTypes(typeDtos(place.getProjectKey(), rows)), indexAssets(rows), false);
            }
        });
    }

    private List<PlaceLayout.Fact> placeFacts(String projectKey) {
        List<PlaceLayout.Fact> facts = new ArrayList<PlaceLayout.Fact>();
        for (AssetTypeDto type : typeDtos(projectKey, assetsIn(projectKey))) {
            if (type == null || type.isLocation()) {
                continue;
            }
            String parent = type.getParentTypeKey() == null ? "" : type.getParentTypeKey().trim();
            facts.add(new PlaceLayout.Fact(type.getTypeKey(), parent));
        }
        return facts;
    }

    private List<String> requestedTypes(PlaceTypeDraft draft) {
        List<String> keys = new ArrayList<String>();
        if (draft != null && draft.getTypeKeys() != null) {
            for (String key : draft.getTypeKeys()) {
                String trimmed = key == null ? "" : key.trim();
                if (!trimmed.isEmpty() && !keys.contains(trimmed)) {
                    keys.add(trimmed);
                }
            }
        }
        if (draft != null && draft.getTypeKey() != null) {
            String trimmed = draft.getTypeKey().trim();
            if (!trimmed.isEmpty() && !keys.contains(trimmed)) {
                keys.add(trimmed);
            }
        }
        if (keys.size() > 40) {
            throw new AssetException(400, "asset-tree.error.type");
        }
        return keys;
    }

    private List<String> offeredLines(String raw) {
        List<String> lines = new ArrayList<String>();
        List<String> seen = new ArrayList<String>();
        if (raw == null || raw.isEmpty()) {
            return lines;
        }
        for (String part : raw.split("\n")) {
            String line = part.trim();
            String key = PlaceLayout.lineKey(line);
            if (key.isEmpty() || seen.contains(key)) {
                continue;
            }
            seen.add(key);
            lines.add(line);
        }
        return lines;
    }

    private List<String> offeredKeys(String raw) {
        List<String> keys = new ArrayList<String>();
        for (String line : offeredLines(raw)) {
            keys.add(PlaceLayout.lineKey(line));
        }
        return keys;
    }

    private String joinOffered(List<String> keys) {
        StringBuilder builder = new StringBuilder();
        for (String key : keys) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(key);
        }
        return builder.toString();
    }

    private boolean placeHasEquipment(AssetEntity place, String typeKey) {
        for (AssetEntity asset : assetsIn(place.getProjectKey())) {
            Integer parent = TreeLogic.normalizeParent(asset.getParentId());
            if (parent != null && parent.intValue() == place.getID()
                    && typeKey.equals(asset.getTypeKey()) && !isLocation(asset)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void applyDuePlans() {
        List<ServiceNotice> notices = new ArrayList<ServiceNotice>();
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                applyDueInside(null, null, notices);
                return null;
            }
        });
        dispatchNotices(notices);
    }

    private void applyDueQuietly(String projectKey, List<ServiceNotice> notices) {
        try {
            applyDueInside(projectKey, null, notices);
        } catch (RuntimeException ex) {
            log.warn("Service dates were not applied", ex);
        }
    }

    private void applyDueFor(String projectKey, AssetEntity[] rows, List<ServiceNotice> notices) {
        Map<Integer, AssetEntity> known = new HashMap<Integer, AssetEntity>();
        if (rows != null) {
            for (AssetEntity row : rows) {
                known.put(Integer.valueOf(row.getID()), row);
            }
        }
        try {
            applyDueInside(projectKey, known, notices);
        } catch (RuntimeException ex) {
            log.warn("Service dates were not applied", ex);
        }
    }

    private void applyDueInside(String projectKey, Map<Integer, AssetEntity> known, List<ServiceNotice> notices) {
        LocalDate today = LocalDate.now();
        ServicePlanEntity[] rows = ao().find(ServicePlanEntity.class);
        if (rows == null) {
            return;
        }
        List<ServicePlanEntity> ordered = new ArrayList<ServicePlanEntity>();
        Collections.addAll(ordered, rows);
        Collections.sort(ordered, new Comparator<ServicePlanEntity>() {
            @Override
            public int compare(ServicePlanEntity left, ServicePlanEntity right) {
                return left.getID() - right.getID();
            }
        });
        Set<Integer> allowed = null;
        if (projectKey != null) {
            allowed = new HashSet<Integer>();
            if (known != null) {
                allowed.addAll(known.keySet());
            } else {
                AssetEntity[] assets = assetsIn(projectKey);
                if (assets != null) {
                    for (AssetEntity asset : assets) {
                        allowed.add(Integer.valueOf(asset.getID()));
                    }
                }
            }
        }
        Map<String, Boolean> serviceOn = new HashMap<String, Boolean>();
        for (ServicePlanEntity plan : ordered) {
            if (allowed != null && !allowed.contains(Integer.valueOf(plan.getAssetId()))) {
                continue;
            }
            try {
                applyOne(plan, known, projectKey, today, notices, serviceOn);
            } catch (RuntimeException ex) {
                log.warn("Service plan {} was skipped", Integer.valueOf(plan.getID()), ex);
            }
        }
    }

    private void applyOne(ServicePlanEntity plan, Map<Integer, AssetEntity> known, String projectKey, LocalDate today,
                           List<ServiceNotice> notices, Map<String, Boolean> serviceOn) {
        AssetEntity asset = known == null ? null : known.get(Integer.valueOf(plan.getAssetId()));
        if (asset == null) {
            asset = ao().get(AssetEntity.class, plan.getAssetId());
        }
        if (asset == null || isLocation(asset)) {
            return;
        }
        Boolean enabled = serviceOn.get(asset.getProjectKey());
        if (enabled == null) {
            enabled = Boolean.valueOf(moduleOn(asset.getProjectKey(), ProjectModules.SERVICE));
            serviceOn.put(asset.getProjectKey(), enabled);
        }
        if (!enabled.booleanValue()) {
            return;
        }
        if (projectKey != null && !projectKey.equals(asset.getProjectKey())) {
            return;
        }
        if ("done".equals(plan.getAppliedFor())) {
            return;
        }
        LocalDate next = ServiceDue.next(plan.getLastDone(), plan.getEveryCount(), plan.getEveryUnit());
        if (!ServiceDue.reached(next, today)) {
            return;
        }
        String dueKey = next.toString();
        if (dueKey.equals(plan.getAppliedFor())) {
            return;
        }
        String statusKey = plan.getStatusKey() == null ? "" : plan.getStatusKey().trim();
        if (!statusKey.isEmpty() && findStatus(asset.getProjectKey(), statusKey) != null) {
            String oldStatus = Statuses.canonical(asset.getStatus());
            if (!statusKey.equals(oldStatus)) {
                asset.setStatus(statusKey);
                asset.setUpdated(new Date());
                asset.setUpdatedBy("");
                asset.save();
                logActivity(asset.getID(), "", "status", "", oldStatus, statusKey);
            }
        }
        recordActivity(asset.getID(), "", "service_due", plan.getName(), plan.getLastDone(), dueKey);
        plan.setAppliedFor(dueKey);
        plan.save();
        if (plan.getNotifyFlag() == 1) {
            ServiceNotice notice = noticeFor(asset, plan, dueKey, statusKey);
            if (notice != null) {
                notices.add(notice);
            }
        }
    }

    private ServiceNotice noticeFor(AssetEntity asset, ServicePlanEntity plan, String dueKey, String statusKey) {
        UserProfileDto person = profile(asset.getCustodianKey());
        if (person == null || person.getEmail() == null || person.getEmail().trim().isEmpty()) {
            return null;
        }
        String statusLabel = statusKey.isEmpty() ? "" : statusText(statusKey);
        ProjectStatusEntity known = statusKey.isEmpty() ? null : findStatus(asset.getProjectKey(), statusKey);
        if (known != null && known.getLabel() != null && !known.getLabel().isEmpty()) {
            statusLabel = known.getLabel();
        }
        String link = jiraBaseUrl() + "/plugins/servlet/asset-tree?project=" + asset.getProjectKey()
                + "&view=all#" + asset.getID();
        String subject = mailText("asset-tree.mail.subject", plan.getName(), asset.getName());
        StringBuilder body = new StringBuilder();
        body.append(asset.getName()).append(" (").append(asset.getObjectKey()).append(")\n");
        body.append(plan.getName()).append(": ").append(plan.getLastDone()).append(" -> ").append(dueKey).append('\n');
        if (!statusLabel.isEmpty()) {
            body.append(statusLabel).append('\n');
        }
        body.append(link);
        return new ServiceNotice(person.getEmail().trim(), subject, body.toString());
    }

    private String mailText(String key, String... args) {
        try {
            I18nHelper labels = i18n();
            if (labels != null) {
                String text;
                if (args == null || args.length == 0) {
                    text = labels.getText(key);
                } else if (args.length == 1) {
                    text = labels.getText(key, args[0]);
                } else {
                    text = labels.getText(key, args[0], args[1]);
                }
                if (text != null && !text.equals(key) && !text.startsWith("asset-tree.")) {
                    return text;
                }
            }
        } catch (RuntimeException ex) {
            log.debug("Service text skipped", ex);
        }
        if ("asset-tree.ui.serviceActor".equals(key)) {
            return "Asset tree";
        }
        if (args != null && args.length >= 2) {
            return args[0] + ": " + args[1];
        }
        return key;
    }

    private void dispatchNotices(List<ServiceNotice> notices) {
        if (notices == null) {
            return;
        }
        for (ServiceNotice notice : notices) {
            try {
                Email email = new Email(notice.email);
                email.setSubject(notice.subject);
                email.setBody(notice.body);
                email.setMimeType("text/plain; charset=UTF-8");
                MailQueue queue = ComponentAccessor.getComponent(MailQueue.class);
                if (queue != null) {
                    queue.addItem(new SingleMailQueueItem(email));
                }
            } catch (RuntimeException ex) {
                log.warn("Service notice was not queued", ex);
            }
        }
    }

    private AssetEntity equipmentForPlan(ApplicationUser user, int assetId) {
        AssetEntity asset = requireAssetCap(user, assetId, GrantCaps.OBJECT);
        if (isLocation(asset)) {
            throw new AssetException(400, "asset-tree.error.service.place");
        }
        return asset;
    }

    private void fillPlan(ServicePlanEntity row, AssetEntity asset, ServicePlanDraft draft, boolean creating) {
        if (draft == null) {
            throw new AssetException(400, "asset-tree.error.service");
        }
        if (Boolean.TRUE.equals(draft.getDone())) {
            row.setCompletedOn(LocalDate.now().toString());
            if (ServiceDue.ONCE.equals(row.getEveryUnit())) {
                row.setAppliedFor("done");
            } else {
                row.setLastDone(LocalDate.now().toString());
                row.setAppliedFor("");
            }
        } else if (draft.getLastDone() != null || creating) {
            String last = FieldDates.canonical(draft.getLastDone());
            if (last == null || last.isEmpty()) {
                throw new AssetException(400, "asset-tree.error.service");
            }
            if (!last.equals(row.getLastDone())) {
                row.setLastDone(last);
                row.setAppliedFor("");
            }
        }
        if (draft.getName() != null || creating) {
            String name = draft.getName() == null ? "" : draft.getName().trim();
            if (name.isEmpty() || name.length() > 80) {
                throw new AssetException(400, "asset-tree.error.service");
            }
            row.setName(name);
        }
        String unit = row.getEveryUnit();
        int count = row.getEveryCount();
        if (draft.getEveryUnit() != null || creating) {
            unit = ServiceDue.unit(draft.getEveryUnit());
        }
        if (draft.getEveryCount() != null || creating) {
            count = draft.getEveryCount() == null ? 0 : draft.getEveryCount().intValue();
        }
        if (unit == null) {
            unit = "";
        }
        if ((draft.getEveryUnit() != null || draft.getEveryCount() != null || creating)
                && !ServiceDue.countAllowed(count, unit)) {
            throw new AssetException(400, "asset-tree.error.service");
        }
        String storedUnit = row.getEveryUnit() == null ? "" : row.getEveryUnit();
        if (!unit.equals(storedUnit) || count != row.getEveryCount()) {
            row.setAppliedFor("");
        }
        row.setEveryUnit(unit);
        row.setEveryCount(count);
        if (draft.getStatusKey() != null || creating) {
            String statusKey = draft.getStatusKey() == null ? "" : draft.getStatusKey().trim();
            if (!statusKey.isEmpty() && findStatus(asset.getProjectKey(), statusKey) == null) {
                throw new AssetException(400, "asset-tree.error.status");
            }
            row.setStatusKey(statusKey);
        }
        if (draft.getNotify() != null || creating) {
            boolean notify = draft.getNotify() == null || draft.getNotify().booleanValue();
            row.setNotifyFlag(notify ? 1 : 0);
        }
        if (draft.getKind() != null || creating) {
            String kind = WorkKind.clean(draft.getKind());
            if (kind.isEmpty()) {
                throw new AssetException(400, "asset-tree.error.service");
            }
            row.setKind(kind);
        }
        if (draft.getAssigneeKey() != null || creating) {
            String assignee = draft.getAssigneeKey() == null ? "" : draft.getAssigneeKey().trim();
            if (!assignee.isEmpty() && userManager().getUserByKey(assignee) == null) {
                throw new AssetException(400, "asset-tree.error.user");
            }
            row.setAssigneeKey(assignee);
        }
        if (draft.getNote() != null || creating) {
            String note = draft.getNote() == null ? "" : draft.getNote().trim();
            if (note.length() > 2000) {
                throw new AssetException(400, "asset-tree.error.service");
            }
            row.setNote(note);
        }
        if (draft.getStartTime() != null || creating) {
            row.setStartTime(clock(draft.getStartTime()));
        }
        if (draft.getEndTime() != null || creating) {
            row.setEndTime(clock(draft.getEndTime()));
        }
        if (row.getName() == null || row.getLastDone() == null || row.getEveryUnit() == null) {
            throw new AssetException(400, "asset-tree.error.service");
        }
    }

    private String clock(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (value.length() != 5 || value.charAt(2) != ':') {
            throw new AssetException(400, "asset-tree.error.service");
        }
        return value;
    }

    private ServicePlanEntity findPlan(int assetId, int planId) {
        ServicePlanEntity row = ao().get(ServicePlanEntity.class, planId);
        if (row == null || row.getAssetId() != assetId) {
            return null;
        }
        return row;
    }

    private List<ServicePlanEntity> plansOf(int assetId) {
        ServicePlanEntity[] rows = ao().find(ServicePlanEntity.class, Query.select().where("ASSET_ID = ?", assetId));
        List<ServicePlanEntity> ordered = new ArrayList<ServicePlanEntity>();
        if (rows != null) {
            Collections.addAll(ordered, rows);
        }
        Collections.sort(ordered, new Comparator<ServicePlanEntity>() {
            @Override
            public int compare(ServicePlanEntity left, ServicePlanEntity right) {
                return left.getID() - right.getID();
            }
        });
        return ordered;
    }

    private List<ServicePlanDto> planDtos(List<ServicePlanEntity> rows, LocalDate today) {
        List<ServicePlanDto> result = new ArrayList<ServicePlanDto>();
        if (rows == null) {
            return result;
        }
        for (ServicePlanEntity row : rows) {
            result.add(planDto(row, today));
        }
        return result;
    }

    private ServicePlanDto planDto(ServicePlanEntity row, LocalDate today) {
        ServicePlanDto dto = new ServicePlanDto();
        dto.setId(row.getID());
        dto.setName(row.getName() == null ? "" : row.getName());
        dto.setLastDone(row.getLastDone() == null ? "" : row.getLastDone());
        dto.setEveryCount(row.getEveryCount());
        dto.setEveryUnit(row.getEveryUnit() == null ? "" : row.getEveryUnit());
        dto.setStatusKey(row.getStatusKey() == null ? "" : row.getStatusKey());
        dto.setNotify(row.getNotifyFlag() == 1);
        dto.setKind(WorkKind.clean(row.getKind()));
        dto.setAssigneeKey(row.getAssigneeKey() == null ? "" : row.getAssigneeKey());
        dto.setAssigneeName(displayName(row.getAssigneeKey()));
        dto.setNote(row.getNote() == null ? "" : row.getNote());
        dto.setStartTime(row.getStartTime() == null ? "" : row.getStartTime());
        dto.setEndTime(row.getEndTime() == null ? "" : row.getEndTime());
        dto.setCompletedOn(row.getCompletedOn() == null ? "" : row.getCompletedOn());
        LocalDate next = ServiceDue.next(row.getLastDone(), row.getEveryCount(), row.getEveryUnit());
        boolean completed = planCompleted(row, next);
        dto.setCompleted(completed);
        dto.setNextDue(next == null ? "" : next.toString());
        dto.setDue(ServiceDue.reached(next, today) && !completed);
        return dto;
    }

    private boolean planCompleted(ServicePlanEntity row, LocalDate next) {
        String applied = row.getAppliedFor() == null ? "" : row.getAppliedFor();
        if ("done".equals(applied)) {
            return true;
        }
        return ServiceDue.ONCE.equals(row.getEveryUnit()) && next != null && next.toString().equals(applied);
    }

    private String dueNames(List<ServicePlanEntity> rows, LocalDate today) {
        StringBuilder names = new StringBuilder();
        if (rows == null) {
            return "";
        }
        for (ServicePlanEntity row : rows) {
            LocalDate next = ServiceDue.next(row.getLastDone(), row.getEveryCount(), row.getEveryUnit());
            if (!ServiceDue.reached(next, today)) {
                continue;
            }
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(row.getName() == null ? "" : row.getName());
        }
        return names.toString();
    }

    private void requireModule(String projectKey, String module) {
        if (!moduleOn(projectKey, module)) {
            throw new AssetException(400, "asset-tree.error.module.off");
        }
    }

    @Override
    public List<ScheduleRowDto> schedule(ApplicationUser user, String projectKey) {
        final Project project = requireReadableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.SERVICE);
        return ao().executeInTransaction(new TransactionCallback<List<ScheduleRowDto>>() {
            @Override
            public List<ScheduleRowDto> doInTransaction() {
                return scheduleRows(project.getKey());
            }
        });
    }

    @Override
    public List<ScheduleRowDto> createSchedule(ApplicationUser user, String projectKey, final ScheduleDraft draft) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.OBJECT);
        requireModule(project.getKey(), ProjectModules.SERVICE);
        if (draft == null || draft.getAssetIds() == null || draft.getAssetIds().isEmpty() || draft.getAssetIds().size() > 40) {
            throw new AssetException(400, "asset-tree.error.service");
        }
        final String kind = WorkKind.clean(draft.getKind());
        if (kind.isEmpty() || !ServiceDue.countAllowed(draft.getEveryCount() == null ? 0 : draft.getEveryCount().intValue(), ServiceDue.unit(draft.getEveryUnit()))) {
            throw new AssetException(400, "asset-tree.error.service");
        }
        String supplied = draft.getName() == null ? "" : draft.getName().trim();
        if (supplied.length() > 80) {
            throw new AssetException(400, "asset-tree.error.service");
        }
        final String name = supplied.isEmpty() ? kindLabel(kind) : supplied;
        final String assignee = draft.getAssigneeKey() == null ? "" : draft.getAssigneeKey().trim();
        if (!assignee.isEmpty() && userManager().getUserByKey(assignee) == null) {
            throw new AssetException(400, "asset-tree.error.user");
        }
        final List<ServiceNotice> notices = new ArrayList<ServiceNotice>();
        List<ScheduleRowDto> created = ao().executeInTransaction(new TransactionCallback<List<ScheduleRowDto>>() {
            @Override
            public List<ScheduleRowDto> doInTransaction() {
                List<ScheduleRowDto> rows = new ArrayList<ScheduleRowDto>();
                LocalDate today = LocalDate.now();
                for (Integer assetId : draft.getAssetIds()) {
                    if (assetId == null) {
                        throw new AssetException(400, "asset-tree.error.service");
                    }
                    AssetEntity asset = ao().get(AssetEntity.class, assetId.intValue());
                    if (asset == null || !ProjectKeys.same(asset.getProjectKey(), project.getKey()) || isLocation(asset)) {
                        throw new AssetException(400, "asset-tree.error.service");
                    }
                    if (plansOf(asset.getID()).size() >= MAX_PLANS) {
                        throw new AssetException(400, "asset-tree.error.service.limit");
                    }
                    ServicePlanDraft plan = new ServicePlanDraft();
                    plan.setName(name);
                    plan.setKind(kind);
                    plan.setAssigneeKey(assignee);
                    plan.setLastDone(ScheduleAnchor.before(draft.getLastDone(),
                            draft.getEveryCount() == null ? 0 : draft.getEveryCount().intValue(), draft.getEveryUnit()));
                    plan.setEveryCount(draft.getEveryCount());
                    plan.setEveryUnit(draft.getEveryUnit());
                    plan.setStatusKey(draft.getStatusKey() == null ? "" : draft.getStatusKey());
                    plan.setNotify(draft.getNotify() == null ? Boolean.FALSE : draft.getNotify());
                    plan.setNote(draft.getNote());
                    plan.setStartTime(draft.getStartTime());
                    plan.setEndTime(draft.getEndTime());
                    ServicePlanEntity row = ao().create(ServicePlanEntity.class, new DBParam("ASSET_ID", asset.getID()));
                    fillPlan(row, asset, plan, true);
                    row.save();
                    rows.add(scheduleRow(row, asset, today));
                }
                applyDueQuietly(project.getKey(), notices);
                return rows;
            }
        });
        dispatchNotices(notices);
        return created;
    }

    @Override
    public List<StaffDutyDto> staff(ApplicationUser user, String projectKey) {
        final Project project = requireReadableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.SERVICE);
        return ao().executeInTransaction(new TransactionCallback<List<StaffDutyDto>>() {
            @Override
            public List<StaffDutyDto> doInTransaction() {
                return staffDtos(project.getKey());
            }
        });
    }

    @Override
    public StaffDutyDto saveStaff(ApplicationUser user, String projectKey, final StaffDutyDraft draft) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.OBJECT);
        requireModule(project.getKey(), ProjectModules.SERVICE);
        final String person = staffUser(draft);
        final int weekdays = draft == null || draft.getWeekdays() == null ? 0 : draft.getWeekdays().intValue();
        final int hours = draft == null || draft.getHours() == null ? 0 : draft.getHours().intValue();
        if (weekdays < 1 || weekdays > 127 || hours < 1 || hours > 24) {
            throw new AssetException(400, "asset-tree.error.staff");
        }
        return ao().executeInTransaction(new TransactionCallback<StaffDutyDto>() {
            @Override
            public StaffDutyDto doInTransaction() {
                List<StaffDutyEntity> rows = dutyRows(project.getKey());
                StaffDutyEntity hoursRow = null;
                for (StaffDutyEntity row : rows) {
                    if ("hours".equals(row.getKind()) && person.equals(row.getUserKey())) {
                        hoursRow = row;
                        break;
                    }
                }
                if (hoursRow == null && rows.size() >= 60) {
                    throw new AssetException(400, "asset-tree.error.staff");
                }
                if (hoursRow == null) {
                    hoursRow = ao().create(StaffDutyEntity.class,
                            new DBParam("PROJECT_KEY", project.getKey()),
                            new DBParam("USER_KEY", person),
                            new DBParam("KIND", "hours"));
                }
                hoursRow.setWeekdays(weekdays);
                hoursRow.setHours(hours);
                hoursRow.setStartOn("");
                hoursRow.setEndOn("");
                hoursRow.save();
                return staffDto(hoursRow);
            }
        });
    }

    @Override
    public StaffDutyDto addStaffLeave(ApplicationUser user, String projectKey, final StaffDutyDraft draft) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.OBJECT);
        requireModule(project.getKey(), ProjectModules.SERVICE);
        final String person = staffUser(draft);
        final LocalDate start = staffDay(draft == null ? null : draft.getStartOn());
        final LocalDate end = staffDay(draft == null ? null : draft.getEndOn());
        if (start == null || end == null || end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 366) {
            throw new AssetException(400, "asset-tree.error.staff");
        }
        final String startText = start.toString();
        final String endText = end.toString();
        return ao().executeInTransaction(new TransactionCallback<StaffDutyDto>() {
            @Override
            public StaffDutyDto doInTransaction() {
                List<StaffDutyEntity> rows = dutyRows(project.getKey());
                boolean known = false;
                int leaves = 0;
                for (StaffDutyEntity row : rows) {
                    if (!person.equals(row.getUserKey())) {
                        continue;
                    }
                    if ("hours".equals(row.getKind())) {
                        known = true;
                    } else if ("leave".equals(row.getKind())) {
                        leaves++;
                    }
                }
                if (!known || leaves >= 24 || rows.size() >= 60) {
                    throw new AssetException(400, "asset-tree.error.staff");
                }
                StaffDutyEntity created = ao().create(StaffDutyEntity.class,
                        new DBParam("PROJECT_KEY", project.getKey()),
                        new DBParam("USER_KEY", person),
                        new DBParam("KIND", "leave"));
                created.setWeekdays(0);
                created.setHours(0);
                created.setStartOn(startText);
                created.setEndOn(endText);
                created.save();
                return staffDto(created);
            }
        });
    }

    @Override
    public void deleteStaff(ApplicationUser user, String projectKey, final int dutyId) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.OBJECT);
        requireModule(project.getKey(), ProjectModules.SERVICE);
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                StaffDutyEntity row = ao().get(StaffDutyEntity.class, dutyId);
                if (row == null || !ProjectKeys.same(row.getProjectKey(), project.getKey())) {
                    throw new AssetException(404, "asset-tree.error.notFound");
                }
                if ("hours".equals(row.getKind())) {
                    List<StaffDutyEntity> own = new ArrayList<StaffDutyEntity>();
                    String person = row.getUserKey();
                    for (StaffDutyEntity item : dutyRows(project.getKey())) {
                        if (person != null && person.equals(item.getUserKey())) {
                            own.add(item);
                        }
                    }
                    if (!own.isEmpty()) {
                        ao().delete(own.toArray(new StaffDutyEntity[own.size()]));
                    }
                } else {
                    ao().delete(row);
                }
                return null;
            }
        });
    }

    private String staffUser(StaffDutyDraft draft) {
        String userKey = draft == null || draft.getUserKey() == null ? "" : draft.getUserKey().trim();
        if (userKey.isEmpty() || userKey.length() > 255 || userManager().getUserByKey(userKey) == null) {
            throw new AssetException(400, "asset-tree.error.user");
        }
        return userKey;
    }

    private LocalDate staffDay(String raw) {
        if (raw == null || !raw.trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private List<StaffDutyEntity> dutyRows(String projectKey) {
        StaffDutyEntity[] found = ao().find(StaffDutyEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
        List<StaffDutyEntity> rows = new ArrayList<StaffDutyEntity>();
        if (found != null) {
            Collections.addAll(rows, found);
        }
        return rows;
    }

    private List<StaffDutyDto> staffDtos(String projectKey) {
        List<StaffDutyDto> rows = new ArrayList<StaffDutyDto>();
        for (StaffDutyEntity row : dutyRows(projectKey)) {
            rows.add(staffDto(row));
        }
        Collections.sort(rows, new Comparator<StaffDutyDto>() {
            @Override
            public int compare(StaffDutyDto left, StaffDutyDto right) {
                int byName = safe(left.getUserName()).compareToIgnoreCase(safe(right.getUserName()));
                if (byName != 0) {
                    return byName;
                }
                int byKind = staffKindOrder(left.getKind()) - staffKindOrder(right.getKind());
                if (byKind != 0) {
                    return byKind;
                }
                return safe(left.getStartOn()).compareTo(safe(right.getStartOn()));
            }
        });
        return rows;
    }

    private int staffKindOrder(String kind) {
        return "hours".equals(kind) ? 0 : 1;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private StaffDutyDto staffDto(StaffDutyEntity row) {
        StaffDutyDto dto = new StaffDutyDto();
        dto.setId(row.getID());
        dto.setUserKey(row.getUserKey() == null ? "" : row.getUserKey());
        dto.setUserName(userLabel(row.getUserKey()));
        dto.setKind(row.getKind() == null ? "" : row.getKind());
        dto.setWeekdays(row.getWeekdays());
        dto.setHours(row.getHours());
        dto.setStartOn(row.getStartOn() == null ? "" : row.getStartOn());
        dto.setEndOn(row.getEndOn() == null ? "" : row.getEndOn());
        return dto;
    }

    private List<ScheduleRowDto> scheduleRows(String projectKey) {
        Map<Integer, AssetEntity> assets = new HashMap<Integer, AssetEntity>();
        for (AssetEntity asset : assetsIn(projectKey)) {
            assets.put(Integer.valueOf(asset.getID()), asset);
        }
        ServicePlanEntity[] plans = ao().find(ServicePlanEntity.class);
        List<ScheduleRowDto> rows = new ArrayList<ScheduleRowDto>();
        LocalDate today = LocalDate.now();
        if (plans != null) {
            for (ServicePlanEntity plan : plans) {
                AssetEntity asset = assets.get(Integer.valueOf(plan.getAssetId()));
                if (asset == null || isLocation(asset)) {
                    continue;
                }
                rows.add(scheduleRow(plan, asset, today));
            }
        }
        return rows;
    }

    private ScheduleRowDto scheduleRow(ServicePlanEntity plan, AssetEntity asset, LocalDate today) {
        ServicePlanDto source = planDto(plan, today);
        ScheduleRowDto row = new ScheduleRowDto();
        row.setId(source.getId());
        row.setAssetId(asset.getID());
        row.setAssetName(asset.getName() == null ? "" : asset.getName());
        row.setObjectKey(asset.getObjectKey() == null ? "" : asset.getObjectKey());
        row.setName(source.getName());
        row.setKind(source.getKind());
        row.setAssigneeKey(source.getAssigneeKey());
        row.setAssigneeName(source.getAssigneeName());
        row.setLastDone(source.getLastDone());
        row.setEveryCount(source.getEveryCount());
        row.setEveryUnit(source.getEveryUnit());
        row.setNextDue(source.getNextDue());
        row.setDue(source.isDue());
        row.setNote(source.getNote());
        row.setStartTime(source.getStartTime());
        row.setEndTime(source.getEndTime());
        row.setCompletedOn(source.getCompletedOn());
        row.setCompleted(source.isCompleted());
        return row;
    }

    private String kindLabel(String kind) {
        if (WorkKind.KTS.equals(kind)) {
            return i18n().getText("asset-tree.ui.kindKts");
        }
        if (WorkKind.VERIFY.equals(kind)) {
            return i18n().getText("asset-tree.ui.kindVerify");
        }
        return i18n().getText("asset-tree.ui.kindTo");
    }

    @Override
    public List<FloorDto> floors(ApplicationUser user, String projectKey) {
        final Project project = requireReadableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.MAP);
        return ao().executeInTransaction(new TransactionCallback<List<FloorDto>>() {
            @Override
            public List<FloorDto> doInTransaction() {
                return floorDtos(project.getKey());
            }
        });
    }

    @Override
    public FloorDto createFloor(ApplicationUser user, String projectKey, final FloorDto draft) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.OBJECT);
        requireModule(project.getKey(), ProjectModules.MAP);
        final String name = floorName(draft == null ? null : draft.getName());
        final int level = draft == null ? 1 : draft.getLevel();
        if (level < -20 || level > 200) {
            throw new AssetException(400, "asset-tree.error.floor");
        }
        return ao().executeInTransaction(new TransactionCallback<FloorDto>() {
            @Override
            public FloorDto doInTransaction() {
                if (floorsOf(project.getKey()).size() >= FloorBounds.MAX_FLOORS) {
                    throw new AssetException(400, "asset-tree.error.floor");
                }
                int placeId = draft == null ? 0 : draft.getPlaceId();
                FloorEntity row = ao().create(FloorEntity.class, new DBParam("PROJECT_KEY", project.getKey()), new DBParam("NAME", name));
                row.setPlaceId(checkedPlace(project.getKey(), placeId));
                row.setLevel(level);
                row.setSortOrder(row.getID());
                row.setPlanW(FloorBounds.width(draft == null ? 0 : draft.getPlanW()));
                row.setPlanH(FloorBounds.height(draft == null ? 0 : draft.getPlanH()));
                row.save();
                return floorDto(row);
            }
        });
    }

    @Override
    public FloorDto saveFloor(ApplicationUser user, String projectKey, final int floorId, final FloorDto draft) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.OBJECT);
        requireModule(project.getKey(), ProjectModules.MAP);
        if (draft == null) {
            throw new AssetException(400, "asset-tree.error.floor");
        }
        final String name = floorName(draft.getName());
        final int planW = FloorBounds.width(draft.getPlanW());
        final int planH = FloorBounds.height(draft.getPlanH());
        if (draft.getLevel() < -20 || draft.getLevel() > 200) {
            throw new AssetException(400, "asset-tree.error.floor");
        }
        List<RoomDto> rooms = draft.getRooms() == null ? new ArrayList<RoomDto>() : draft.getRooms();
        List<PinDto> pins = draft.getPins() == null ? new ArrayList<PinDto>() : draft.getPins();
        if (rooms.size() > FloorBounds.MAX_ROOMS || pins.size() > FloorBounds.MAX_PINS) {
            throw new AssetException(400, "asset-tree.error.floor");
        }
        return ao().executeInTransaction(new TransactionCallback<FloorDto>() {
            @Override
            public FloorDto doInTransaction() {
                FloorEntity row = requireFloor(project.getKey(), floorId);
                row.setName(name);
                row.setLevel(draft.getLevel());
                row.setPlaceId(checkedPlace(project.getKey(), draft.getPlaceId()));
                row.setPlanW(planW);
                row.setPlanH(planH);
                row.save();
                ao().deleteWithSQL(RoomEntity.class, "FLOOR_ID = ?", Integer.valueOf(row.getID()));
                ao().deleteWithSQL(PinEntity.class, "FLOOR_ID = ?", Integer.valueOf(row.getID()));
                Set<Integer> seen = new HashSet<Integer>();
                if (draft.getRooms() != null) {
                    for (RoomDto room : draft.getRooms()) {
                        if (room == null) {
                            continue;
                        }
                        String kind = FloorBounds.kind(room.getKind());
                        int min = FloorBounds.minSize(kind);
                        int x = FloorBounds.clamp(room.getX(), planW - min);
                        int y = FloorBounds.clamp(room.getY(), planH - min);
                        int w = Math.max(min, room.getW());
                        int h = Math.max(min, room.getH());
                        if (x + w > planW) {
                            w = planW - x;
                        }
                        if (y + h > planH) {
                            h = planH - y;
                        }
                        RoomEntity stored = ao().create(RoomEntity.class, new DBParam("FLOOR_ID", row.getID()));
                        stored.setName(roomName(room.getName(), kind));
                        stored.setKind(kind);
                        stored.setColor(FloorBounds.color(room.getColor()));
                        stored.setPosX(x);
                        stored.setPosY(y);
                        stored.setPosW(w);
                        stored.setPosH(h);
                        stored.save();
                    }
                }
                if (draft.getPins() != null) {
                    for (PinDto pin : draft.getPins()) {
                        if (pin == null || !seen.add(Integer.valueOf(pin.getAssetId()))) {
                            continue;
                        }
                        AssetEntity asset = ao().get(AssetEntity.class, pin.getAssetId());
                        if (asset == null || !ProjectKeys.same(asset.getProjectKey(), project.getKey()) || isLocation(asset)) {
                            throw new AssetException(400, "asset-tree.error.floor");
                        }
                        PinEntity stored = ao().create(PinEntity.class, new DBParam("FLOOR_ID", row.getID()), new DBParam("ASSET_ID", asset.getID()));
                        stored.setPosX(FloorBounds.clamp(pin.getX(), planW));
                        stored.setPosY(FloorBounds.clamp(pin.getY(), planH));
                        stored.save();
                    }
                }
                return floorDto(row);
            }
        });
    }

    @Override
    public void deleteFloor(ApplicationUser user, String projectKey, final int floorId) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.OBJECT);
        requireModule(project.getKey(), ProjectModules.MAP);
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                FloorEntity row = requireFloor(project.getKey(), floorId);
                ao().deleteWithSQL(RoomEntity.class, "FLOOR_ID = ?", Integer.valueOf(row.getID()));
                ao().deleteWithSQL(PinEntity.class, "FLOOR_ID = ?", Integer.valueOf(row.getID()));
                ao().delete(row);
                return null;
            }
        });
    }

    private String roomName(String raw, String kind) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            return FloorBounds.ROOM.equals(kind) ? i18n().getText("asset-tree.ui.mapRoom") : "";
        }
        if (name.length() > 80) {
            throw new AssetException(400, "asset-tree.error.floor");
        }
        return name;
    }

    private String floorName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.length() > 80) {
            throw new AssetException(400, "asset-tree.error.floor");
        }
        return name;
    }

    private int checkedPlace(String projectKey, int placeId) {
        if (placeId <= 0) {
            return 0;
        }
        AssetEntity place = ao().get(AssetEntity.class, placeId);
        if (place == null || !ProjectKeys.same(place.getProjectKey(), projectKey) || !isLocation(place)) {
            throw new AssetException(400, "asset-tree.error.floor");
        }
        return place.getID();
    }

    private FloorEntity requireFloor(String projectKey, int floorId) {
        FloorEntity row = ao().get(FloorEntity.class, floorId);
        if (row == null || !ProjectKeys.same(row.getProjectKey(), projectKey)) {
            throw new AssetException(404, "asset-tree.error.notFound");
        }
        return row;
    }

    private List<FloorEntity> floorsOf(String projectKey) {
        FloorEntity[] rows = ao().find(FloorEntity.class);
        List<FloorEntity> found = new ArrayList<FloorEntity>();
        if (rows != null) {
            for (FloorEntity row : rows) {
                if (ProjectKeys.same(row.getProjectKey(), projectKey)) {
                    found.add(row);
                }
            }
        }
        Collections.sort(found, new Comparator<FloorEntity>() {
            @Override
            public int compare(FloorEntity left, FloorEntity right) {
                int byLevel = left.getLevel() - right.getLevel();
                return byLevel != 0 ? byLevel : left.getID() - right.getID();
            }
        });
        return found;
    }

    private List<FloorDto> floorDtos(String projectKey) {
        List<FloorDto> result = new ArrayList<FloorDto>();
        for (FloorEntity row : floorsOf(projectKey)) {
            result.add(floorDto(row));
        }
        return result;
    }

    private FloorDto floorDto(FloorEntity row) {
        FloorDto dto = new FloorDto();
        dto.setId(row.getID());
        dto.setName(row.getName() == null ? "" : row.getName());
        dto.setLevel(row.getLevel());
        dto.setPlaceId(row.getPlaceId());
        if (row.getPlaceId() > 0) {
            AssetEntity place = ao().get(AssetEntity.class, row.getPlaceId());
            dto.setPlaceName(place == null || place.getName() == null ? "" : place.getName());
        } else {
            dto.setPlaceName("");
        }
        dto.setPlanW(FloorBounds.width(row.getPlanW()));
        dto.setPlanH(FloorBounds.height(row.getPlanH()));
        RoomEntity[] rooms = ao().find(RoomEntity.class, Query.select().where("FLOOR_ID = ?", row.getID()));
        List<RoomDto> roomDtos = new ArrayList<RoomDto>();
        if (rooms != null) {
            for (RoomEntity room : rooms) {
                RoomDto item = new RoomDto();
                item.setId(room.getID());
                item.setName(room.getName() == null ? "" : room.getName());
                item.setX(room.getPosX());
                item.setY(room.getPosY());
                item.setW(room.getPosW());
                item.setH(room.getPosH());
                item.setKind(FloorBounds.kind(room.getKind()));
                item.setColor(FloorBounds.color(room.getColor()));
                roomDtos.add(item);
            }
        }
        dto.setRooms(roomDtos);
        PinEntity[] pins = ao().find(PinEntity.class, Query.select().where("FLOOR_ID = ?", row.getID()));
        List<PinDto> pinDtos = new ArrayList<PinDto>();
        if (pins != null) {
            for (PinEntity pin : pins) {
                AssetEntity asset = ao().get(AssetEntity.class, pin.getAssetId());
                PinDto item = new PinDto();
                item.setId(pin.getID());
                item.setAssetId(pin.getAssetId());
                item.setAssetName(asset == null || asset.getName() == null ? "" : asset.getName());
                item.setObjectKey(asset == null || asset.getObjectKey() == null ? "" : asset.getObjectKey());
                item.setX(pin.getPosX());
                item.setY(pin.getPosY());
                pinDtos.add(item);
            }
        }
        dto.setPins(pinDtos);
        return dto;
    }

    @Override
    public List<SavedReportDto> reports(ApplicationUser user, String projectKey) {
        final Project project = requireReadableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.REPORTS);
        return ao().executeInTransaction(new TransactionCallback<List<SavedReportDto>>() {
            @Override
            public List<SavedReportDto> doInTransaction() {
                return reportCatalog(project.getKey());
            }
        });
    }

    @Override
    public SavedReportDto createReport(ApplicationUser user, String projectKey, final SavedReportDraft draft) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.OBJECT);
        requireModule(project.getKey(), ProjectModules.REPORTS);
        if (draft == null) {
            throw new AssetException(400, "asset-tree.error.report");
        }
        final String name = draft.getName() == null ? "" : draft.getName().trim();
        final String group = ReportCatalog.group(draft.getGroupBy());
        final String filter = reportFilter(draft.getFilterField());
        final String value = draft.getFilterValue() == null ? "" : draft.getFilterValue().trim();
        final String chart = ReportCatalog.chart(draft.getChart());
        final String metric = reportMetric(draft.getMetric());
        if (name.isEmpty() || name.length() > 80 || group.isEmpty() || (filter.isEmpty() != value.isEmpty()) || value.length() > 255) {
            throw new AssetException(400, "asset-tree.error.report");
        }
        if ("mix".equals(filter) && !mixLooksValid(value)) {
            throw new AssetException(400, "asset-tree.error.report");
        }
        return ao().executeInTransaction(new TransactionCallback<SavedReportDto>() {
            @Override
            public SavedReportDto doInTransaction() {
                SavedReportEntity row = ao().create(SavedReportEntity.class,
                        new DBParam("PROJECT_KEY", project.getKey()),
                        new DBParam("NAME", name),
                        new DBParam("GROUP_BY", group));
                row.setFilterField(filter);
                row.setFilterValue(value);
                row.setChart(chart.isEmpty() ? "bar" : chart);
                row.setMetric(metric);
                row.save();
                return savedReportDto(row);
            }
        });
    }

    @Override
    public void deleteReport(ApplicationUser user, String projectKey, String reportKey) {
        final Project project = requireProjectCap(user, projectKey, GrantCaps.OBJECT);
        requireModule(project.getKey(), ProjectModules.REPORTS);
        final int id = ReportCatalog.savedId(reportKey);
        if (id <= 0) {
            throw new AssetException(400, "asset-tree.error.report");
        }
        ao().executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                SavedReportEntity row = ao().get(SavedReportEntity.class, id);
                if (row == null || !ProjectKeys.same(row.getProjectKey(), project.getKey())) {
                    throw new AssetException(404, "asset-tree.error.notFound");
                }
                ao().delete(row);
                return null;
            }
        });
    }

    @Override
    public ReportViewDto reportView(ApplicationUser user, final String projectKey, final String reportKey) {
        final Project project = requireReadableProject(user, projectKey);
        requireModule(project.getKey(), ProjectModules.REPORTS);
        return ao().executeInTransaction(new TransactionCallback<ReportViewDto>() {
            @Override
            public ReportViewDto doInTransaction() {
                String group = ReportCatalog.group(reportKey);
                String filter = "";
                String value = "";
                String chart = "";
                String title;
                boolean standard = true;
                String key = reportKey == null ? "" : reportKey.trim().toLowerCase(Locale.ROOT);
                if (ReportCatalog.isStandard(key)) {
                    title = standardTitle(key);
                    group = standardGroup(key);
                    chart = ReportCatalog.STATUS.equals(key) ? "donut" : "bar";
                } else {
                    SavedReportEntity saved = ao().get(SavedReportEntity.class, ReportCatalog.savedId(key));
                    if (saved == null || !ProjectKeys.same(saved.getProjectKey(), project.getKey())) {
                        throw new AssetException(404, "asset-tree.error.notFound");
                    }
                    standard = false;
                    title = saved.getName();
                    group = saved.getGroupBy();
                    filter = saved.getFilterField() == null ? "" : saved.getFilterField();
                    value = saved.getFilterValue() == null ? "" : saved.getFilterValue();
                    chart = ReportCatalog.chart(saved.getChart());
                    if (chart.isEmpty()) {
                        chart = "bar";
                    }
                    key = ReportCatalog.savedKey(saved.getID());
                }
                ReportViewDto view = buildReport(project, key, title, standard, group, filter, value);
                view.setGroupBy(group);
                view.setChart(chart);
                return view;
            }
        });
    }

    private List<SavedReportDto> reportCatalog(String projectKey) {
        List<SavedReportDto> items = new ArrayList<SavedReportDto>();
        for (String key : ReportCatalog.standard()) {
            SavedReportDto item = new SavedReportDto();
            item.setId(key);
            item.setName(standardTitle(key));
            item.setGroupBy(standardGroup(key));
            item.setChart(ReportCatalog.STATUS.equals(key) ? "donut" : "bar");
            item.setMetric("count");
            item.setStandard(true);
            items.add(item);
        }
        SavedReportEntity[] rows = ao().find(SavedReportEntity.class);
        if (rows != null) {
            List<SavedReportEntity> own = new ArrayList<SavedReportEntity>();
            for (SavedReportEntity row : rows) {
                if (ProjectKeys.same(row.getProjectKey(), projectKey)) {
                    own.add(row);
                }
            }
            Collections.sort(own, new Comparator<SavedReportEntity>() {
                @Override
                public int compare(SavedReportEntity left, SavedReportEntity right) {
                    return left.getID() - right.getID();
                }
            });
            for (SavedReportEntity row : own) {
                items.add(savedReportDto(row));
            }
        }
        return items;
    }

    private SavedReportDto savedReportDto(SavedReportEntity row) {
        SavedReportDto dto = new SavedReportDto();
        dto.setId(ReportCatalog.savedKey(row.getID()));
        dto.setName(row.getName() == null ? "" : row.getName());
        dto.setGroupBy(row.getGroupBy() == null ? "" : row.getGroupBy());
        dto.setFilterField(row.getFilterField() == null ? "" : row.getFilterField());
        dto.setFilterValue(row.getFilterValue() == null ? "" : row.getFilterValue());
        dto.setChart(ReportCatalog.chart(row.getChart()).isEmpty() ? "bar" : ReportCatalog.chart(row.getChart()));
        dto.setMetric(reportMetric(row.getMetric()));
        dto.setStandard(false);
        return dto;
    }

    private String reportMetric(String raw) {
        if (raw != null && "cost".equals(raw.trim().toLowerCase(Locale.ROOT))) {
            return "cost";
        }
        return "count";
    }

    private boolean mixLooksValid(String value) {
        return value.startsWith("t=") || value.startsWith("s=") || value.contains(";t=") || value.contains(";s=");
    }

    private String standardGroup(String key) {
        if (ReportCatalog.STATUS.equals(key) || ReportCatalog.TYPE.equals(key)
                || ReportCatalog.PLACE.equals(key) || ReportCatalog.HOLDER.equals(key)) {
            return key;
        }
        if (ReportCatalog.COST.equals(key)) {
            return ReportCatalog.TYPE;
        }
        if (ReportCatalog.RECEIPTS.equals(key)) {
            return "month";
        }
        if (ReportCatalog.AGE.equals(key)) {
            return "age";
        }
        if (ReportCatalog.WARRANTY.equals(key)) {
            return "warranty";
        }
        if (ReportCatalog.WRITEOFF.equals(key)) {
            return "writeoff";
        }
        if (ReportCatalog.WORK.equals(key) || ReportCatalog.SERVICE.equals(key)) {
            return "kind";
        }
        return "";
    }

    private String reportFilter(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (ReportCatalog.STATUS.equals(value) || ReportCatalog.TYPE.equals(value) || ReportCatalog.HOLDER.equals(value) || "mix".equals(value)) {
            return value;
        }
        return "";
    }

    private String standardTitle(String key) {
        if (ReportCatalog.TYPE.equals(key)) {
            return i18n().getText("asset-tree.ui.reportTypeTitle");
        }
        if (ReportCatalog.PLACE.equals(key)) {
            return i18n().getText("asset-tree.ui.reportPlaceTitle");
        }
        if (ReportCatalog.HOLDER.equals(key)) {
            return i18n().getText("asset-tree.ui.reportHolderTitle");
        }
        if (ReportCatalog.SERVICE.equals(key)) {
            return i18n().getText("asset-tree.ui.reportService");
        }
        if (ReportCatalog.WARRANTY.equals(key)) {
            return i18n().getText("asset-tree.ui.reportWarrantyTitle");
        }
        if (ReportCatalog.COST.equals(key)) {
            return i18n().getText("asset-tree.ui.reportCostTitle");
        }
        if (ReportCatalog.RECEIPTS.equals(key)) {
            return i18n().getText("asset-tree.ui.reportReceipts");
        }
        if (ReportCatalog.AGE.equals(key)) {
            return i18n().getText("asset-tree.ui.reportAge");
        }
        if (ReportCatalog.WRITEOFF.equals(key)) {
            return i18n().getText("asset-tree.ui.reportWriteoffs");
        }
        if (ReportCatalog.WORK.equals(key)) {
            return i18n().getText("asset-tree.ui.reportWork");
        }
        return i18n().getText("asset-tree.ui.reportStatusTitle");
    }

    private ReportViewDto buildReport(Project project, String key, String title, boolean standard,
            String group, String filter, String value) {
        AssetEntity[] assets = assetsIn(project.getKey());
        Map<String, AssetTypeDto> types = indexTypes(typeDtos(project.getKey(), assets));
        Map<Integer, AssetEntity> index = new HashMap<Integer, AssetEntity>();
        for (AssetEntity asset : assets) {
            index.put(Integer.valueOf(asset.getID()), asset);
        }
        Map<Integer, LifecycleEntity> lives = lifeIndex();
        Map<Integer, ServicePlanEntity> nextPlan = nextPlanIndex();
        List<AssetEntity> chosen = new ArrayList<AssetEntity>();
        for (AssetEntity asset : assets) {
            AssetTypeDto type = types.get(asset.getTypeKey());
            if (type != null && type.isLocation()) {
                continue;
            }
            if (!reportFilterMatches(asset, filter, value)) {
                continue;
            }
            if ((ReportCatalog.SERVICE.equals(key) || ReportCatalog.WORK.equals(key)) && !nextPlan.containsKey(Integer.valueOf(asset.getID()))) {
                continue;
            }
            LifecycleEntity life = lives.get(Integer.valueOf(asset.getID()));
            if (ReportCatalog.WARRANTY.equals(key) && (life == null || life.getWarrantyUntil() == null || life.getWarrantyUntil().isEmpty())) {
                continue;
            }
            if (ReportCatalog.COST.equals(key) && (life == null || life.getPriceMinor() <= 0L)) {
                continue;
            }
            if ((ReportCatalog.RECEIPTS.equals(key) || ReportCatalog.AGE.equals(key)) && (life == null || FieldDates.canonical(life.getPurchased()) == null || FieldDates.canonical(life.getPurchased()).isEmpty())) {
                continue;
            }
            if (ReportCatalog.WRITEOFF.equals(key) && !writtenOff(asset, life)) {
                continue;
            }
            chosen.add(asset);
        }
        Map<String, ReportBucketDto> buckets = new LinkedHashMap<String, ReportBucketDto>();
        List<ReportLineDto> lines = new ArrayList<ReportLineDto>();
        long sum = 0L;
        LocalDate today = LocalDate.now();
        for (AssetEntity asset : chosen) {
            String bucketKey = reportBucketKey(key, group, asset, types, index, lives, nextPlan, today);
            String label = reportBucketLabel(key, group, bucketKey, asset, types, index);
            ReportBucketDto bucket = buckets.get(bucketKey);
            if (bucket == null) {
                bucket = new ReportBucketDto(bucketKey, label, reportColor(key, group, bucketKey, types), 0);
                buckets.put(bucketKey, bucket);
            }
            bucket.setCount(bucket.getCount() + 1);
            String detail = reportDetail(key, asset, lives, nextPlan, today);
            if (ReportCatalog.COST.equals(key)) {
                sum += bookOf(lives.get(Integer.valueOf(asset.getID())), today);
            }
            if (lines.size() < 300) {
                lines.add(new ReportLineDto(asset.getID(), asset.getObjectKey(), asset.getName(), detail));
            }
        }
        ReportViewDto view = new ReportViewDto();
        view.setId(key);
        view.setTitle(title);
        view.setHint(i18n().getText("asset-tree.ui.reportCounted"));
        view.setStandard(standard);
        view.setTotal(chosen.size());
        view.setRows(new ArrayList<ReportBucketDto>(buckets.values()));
        view.setLines(lines);
        view.setSum(ReportCatalog.COST.equals(key) ? MoneyText.format(sum) : "");
        return view;
    }

    private boolean reportFilterMatches(AssetEntity asset, String filter, String value) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        if ("mix".equals(filter)) {
            String types = mixPart(value, "t");
            String statuses = mixPart(value, "s");
            if (!types.isEmpty() && !tokenHit(types, asset.getTypeKey())) {
                return false;
            }
            return statuses.isEmpty() || tokenHit(statuses, Statuses.canonical(asset.getStatus()));
        }
        if (ReportCatalog.STATUS.equals(filter)) {
            return tokenHit(value, Statuses.canonical(asset.getStatus()));
        }
        if (ReportCatalog.TYPE.equals(filter)) {
            return tokenHit(value, asset.getTypeKey());
        }
        return tokenHit(value, asset.getCustodianKey() == null ? "" : asset.getCustodianKey());
    }

    private String mixPart(String value, String name) {
        if (value == null) {
            return "";
        }
        String[] parts = value.split(";");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.startsWith(name + "=")) {
                return part.substring(name.length() + 1);
            }
        }
        return "";
    }

    private boolean tokenHit(String list, String token) {
        if (token == null) {
            token = "";
        }
        if (list == null || list.isEmpty()) {
            return false;
        }
        if (list.indexOf(',') < 0) {
            return list.equals(token);
        }
        String[] parts = list.split(",");
        for (int i = 0; i < parts.length; i++) {
            if (token.equals(parts[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean writtenOff(AssetEntity asset, LifecycleEntity life) {
        if (Statuses.WRITTEN_OFF.equals(Statuses.canonical(asset.getStatus()))) {
            return true;
        }
        return life != null && "approved".equals(life.getWriteoffState());
    }

    private String reportBucketKey(String reportKey, String group, AssetEntity asset, Map<String, AssetTypeDto> types,
            Map<Integer, AssetEntity> index, Map<Integer, LifecycleEntity> lives, Map<Integer, ServicePlanEntity> plans, LocalDate today) {
        if (ReportCatalog.SERVICE.equals(reportKey) || ReportCatalog.WORK.equals(reportKey)) {
            ServicePlanEntity plan = plans.get(Integer.valueOf(asset.getID()));
            return plan == null ? WorkKind.TO : WorkKind.clean(plan.getKind());
        }
        if (ReportCatalog.WARRANTY.equals(reportKey)) {
            return warrantyBand(lives.get(Integer.valueOf(asset.getID())), today);
        }
        if (ReportCatalog.RECEIPTS.equals(reportKey)) {
            LifecycleEntity bought = lives.get(Integer.valueOf(asset.getID()));
            String iso = bought == null ? "" : FieldDates.canonical(bought.getPurchased());
            return iso == null || iso.length() < 7 ? "" : iso.substring(0, 7);
        }
        if (ReportCatalog.AGE.equals(reportKey)) {
            return ageBand(lives.get(Integer.valueOf(asset.getID())), today);
        }
        if (ReportCatalog.WRITEOFF.equals(reportKey)) {
            LifecycleEntity life = lives.get(Integer.valueOf(asset.getID()));
            String decided = life == null ? "" : FieldDates.canonical(life.getDecidedAt());
            if (decided == null || decided.length() < 7) {
                return "none";
            }
            return decided.substring(0, 7);
        }
        if (ReportCatalog.COST.equals(reportKey)) {
            return asset.getTypeKey() == null ? "" : asset.getTypeKey();
        }
        if (ReportCatalog.TYPE.equals(group)) {
            return asset.getTypeKey() == null ? "" : asset.getTypeKey();
        }
        if (ReportCatalog.PLACE.equals(group)) {
            return placeKey(asset, types, index);
        }
        if (ReportCatalog.HOLDER.equals(group)) {
            return asset.getCustodianKey() == null ? "" : asset.getCustodianKey();
        }
        return Statuses.canonical(asset.getStatus());
    }

    private String reportBucketLabel(String reportKey, String group, String bucketKey, AssetEntity asset,
            Map<String, AssetTypeDto> types, Map<Integer, AssetEntity> index) {
        if (ReportCatalog.SERVICE.equals(reportKey) || ReportCatalog.WORK.equals(reportKey)) {
            return kindLabel(bucketKey);
        }
        if (ReportCatalog.AGE.equals(reportKey)) {
            if ("y0".equals(bucketKey)) {
                return i18n().getText("asset-tree.ui.reportAge0");
            }
            if ("y1".equals(bucketKey)) {
                return i18n().getText("asset-tree.ui.reportAge1");
            }
            if ("y3".equals(bucketKey)) {
                return i18n().getText("asset-tree.ui.reportAge3");
            }
            return i18n().getText("asset-tree.ui.reportAge5");
        }
        if (ReportCatalog.WARRANTY.equals(reportKey)) {
            if ("expired".equals(bucketKey)) {
                return i18n().getText("asset-tree.ui.lifeWarrantyOff");
            }
            if ("soon".equals(bucketKey)) {
                return i18n().getText("asset-tree.ui.lifeWarrantySoon");
            }
            return i18n().getText("asset-tree.ui.lifeWarrantyOn");
        }
        if (ReportCatalog.TYPE.equals(group) || ReportCatalog.COST.equals(reportKey)) {
            AssetTypeDto type = types.get(bucketKey);
            return type == null ? bucketKey : type.getLabel();
        }
        if (ReportCatalog.PLACE.equals(group)) {
            if (bucketKey.isEmpty()) {
                return i18n().getText("asset-tree.ui.root");
            }
            AssetEntity place = index.get(Integer.valueOf(bucketKey));
            return place == null || place.getName() == null ? bucketKey : place.getName();
        }
        if (ReportCatalog.HOLDER.equals(group)) {
            return bucketKey.isEmpty() ? i18n().getText("asset-tree.ui.reportUnassigned") : displayName(bucketKey);
        }
        return statusText(bucketKey);
    }

    private String reportColor(String reportKey, String group, String bucketKey, Map<String, AssetTypeDto> types) {
        if (ReportCatalog.SERVICE.equals(reportKey) || ReportCatalog.WORK.equals(reportKey) || "kind".equals(group)) {
            if (WorkKind.KTS.equals(bucketKey)) {
                return "#FF8B00";
            }
            if (WorkKind.VERIFY.equals(bucketKey)) {
                return "#6554C0";
            }
            return "#0052CC";
        }
        if (ReportCatalog.STATUS.equals(group) || ReportCatalog.STATUS.equals(reportKey)) {
            return statusColor(bucketKey);
        }
        if ("expired".equals(bucketKey)) {
            return "#DE350B";
        }
        if ("soon".equals(bucketKey)) {
            return "#FF8B00";
        }
        if (ReportCatalog.TYPE.equals(group) || ReportCatalog.COST.equals(reportKey)) {
            AssetTypeDto type = types.get(bucketKey);
            return type == null || type.getColor() == null ? "#0052CC" : type.getColor();
        }
        return "#0052CC";
    }

    private String statusColor(String key) {
        if ("in_use".equals(key) || "active".equals(key)) {
            return "#00875A";
        }
        if ("repair".equals(key) || "maintenance".equals(key)) {
            return "#FF8B00";
        }
        if (Statuses.WRITTEN_OFF.equals(key) || "retired".equals(key)) {
            return "#DE350B";
        }
        if ("reserve".equals(key) || "inactive".equals(key)) {
            return "#6554C0";
        }
        if ("in_stock".equals(key)) {
            return "#6B778C";
        }
        return "#0052CC";
    }

    private String ageBand(LifecycleEntity life, LocalDate today) {
        String iso = life == null ? "" : FieldDates.canonical(life.getPurchased());
        if (iso == null || iso.isEmpty()) {
            return "y0";
        }
        long months = Depreciation.elapsedMonths(LocalDate.parse(iso), today);
        if (months < 12) {
            return "y0";
        }
        if (months < 36) {
            return "y1";
        }
        if (months < 60) {
            return "y3";
        }
        return "y5";
    }

    private String reportDetail(String reportKey, AssetEntity asset, Map<Integer, LifecycleEntity> lives,
            Map<Integer, ServicePlanEntity> plans, LocalDate today) {
        if (ReportCatalog.SERVICE.equals(reportKey) || ReportCatalog.WORK.equals(reportKey)) {
            ServicePlanEntity plan = plans.get(Integer.valueOf(asset.getID()));
            if (plan == null) {
                return "";
            }
            LocalDate next = ServiceDue.next(plan.getLastDone(), plan.getEveryCount(), plan.getEveryUnit());
            return kindLabel(WorkKind.clean(plan.getKind())) + (next == null ? "" : " · " + next.toString());
        }
        LifecycleEntity life = lives.get(Integer.valueOf(asset.getID()));
        if (life == null) {
            return "";
        }
        if (ReportCatalog.WARRANTY.equals(reportKey)) {
            return life.getWarrantyUntil() == null ? "" : life.getWarrantyUntil();
        }
        if (ReportCatalog.COST.equals(reportKey)) {
            return MoneyText.format(bookOf(life, today));
        }
        return "";
    }

    private String placeKey(AssetEntity asset, Map<String, AssetTypeDto> types, Map<Integer, AssetEntity> index) {
        Integer parent = asset.getParentId();
        int guard = 0;
        while (parent != null && parent.intValue() > 0 && guard < 40) {
            AssetEntity node = index.get(parent);
            if (node == null) {
                break;
            }
            AssetTypeDto type = types.get(node.getTypeKey());
            if (type != null && type.isLocation()) {
                return String.valueOf(node.getID());
            }
            parent = node.getParentId();
            guard++;
        }
        return "";
    }

    private String warrantyBand(LifecycleEntity life, LocalDate today) {
        String raw = life == null ? "" : life.getWarrantyUntil();
        String iso = FieldDates.canonical(raw);
        if (iso == null || iso.isEmpty()) {
            return "expired";
        }
        LocalDate until = LocalDate.parse(iso);
        if (!Depreciation.warrantyActive(until, today)) {
            return "expired";
        }
        if (!until.isAfter(today.plusDays(90))) {
            return "soon";
        }
        return "active";
    }

    private long bookOf(LifecycleEntity life, LocalDate today) {
        if (life == null) {
            return 0L;
        }
        String iso = FieldDates.canonical(life.getPurchased());
        LocalDate purchased = iso == null || iso.isEmpty() ? null : LocalDate.parse(iso);
        return Depreciation.bookMinor(life.getPriceMinor(), life.getResidualMinor(), life.getLifeMonths(),
                Depreciation.elapsedMonths(purchased, today));
    }

    private Map<Integer, LifecycleEntity> lifeIndex() {
        Map<Integer, LifecycleEntity> index = new HashMap<Integer, LifecycleEntity>();
        LifecycleEntity[] rows = ao().find(LifecycleEntity.class);
        if (rows != null) {
            for (LifecycleEntity row : rows) {
                index.put(Integer.valueOf(row.getAssetId()), row);
            }
        }
        return index;
    }

    private Map<Integer, ServicePlanEntity> nextPlanIndex() {
        Map<Integer, ServicePlanEntity> index = new HashMap<Integer, ServicePlanEntity>();
        ServicePlanEntity[] rows = ao().find(ServicePlanEntity.class);
        LocalDate today = LocalDate.now();
        if (rows != null) {
            for (ServicePlanEntity row : rows) {
                Integer id = Integer.valueOf(row.getAssetId());
                ServicePlanEntity current = index.get(id);
                if (current == null || sooner(row, current, today)) {
                    index.put(id, row);
                }
            }
        }
        return index;
    }

    private boolean sooner(ServicePlanEntity candidate, ServicePlanEntity current, LocalDate today) {
        LocalDate left = ServiceDue.next(candidate.getLastDone(), candidate.getEveryCount(), candidate.getEveryUnit());
        LocalDate right = ServiceDue.next(current.getLastDone(), current.getEveryCount(), current.getEveryUnit());
        if (left == null) {
            return false;
        }
        if (right == null) {
            return true;
        }
        return left.isBefore(right);
    }

    @Override
    public LifecycleDto lifecycle(ApplicationUser user, final int assetId) {
        final AssetEntity asset = equipmentForLife(user, assetId, false);
        return ao().executeInTransaction(new TransactionCallback<LifecycleDto>() {
            @Override
            public LifecycleDto doInTransaction() {
                return lifecycleDto(asset, lifeRow(asset.getID()));
            }
        });
    }

    @Override
    public LifecycleDto saveLifecycle(ApplicationUser user, final int assetId, final LifecycleDraft draft) {
        final AssetEntity asset = equipmentForLife(user, assetId, true);
        if (draft == null) {
            throw new AssetException(400, "asset-tree.error.life");
        }
        return ao().executeInTransaction(new TransactionCallback<LifecycleDto>() {
            @Override
            public LifecycleDto doInTransaction() {
                LifecycleEntity row = ensureLife(asset.getID());
                if ("approved".equals(row.getWriteoffState())) {
                    throw new AssetException(400, "asset-tree.error.writeoff.closed");
                }
                String purchased = FieldDates.canonical(draft.getPurchased());
                String warranty = FieldDates.canonical(draft.getWarrantyUntil());
                String commissioned = FieldDates.canonical(draft.getCommissioned());
                if (purchased == null || warranty == null || commissioned == null) {
                    throw new AssetException(400, "asset-tree.error.life");
                }
                Long price = MoneyText.parse(draft.getPrice());
                Long residual = MoneyText.parse(draft.getResidual());
                int months = draft.getLifeMonths() == null ? 0 : draft.getLifeMonths().intValue();
                String supplier = draft.getSupplier() == null ? "" : draft.getSupplier().trim();
                String document = draft.getDocument() == null ? "" : draft.getDocument().trim();
                String service = draft.getWarrantyService() == null ? "" : draft.getWarrantyService().trim();
                String currency = draft.getCurrency() == null ? "" : draft.getCurrency().trim().toUpperCase(Locale.ROOT);
                if (currency.isEmpty()) {
                    currency = "RUB";
                }
                if (price == null || residual == null || months < 0 || months > 600 || supplier.length() > 120
                        || document.length() > 160 || service.length() > 160 || currency.length() != 3
                        || residual.longValue() > price.longValue()) {
                    throw new AssetException(400, "asset-tree.error.life");
                }
                row.setPurchased(purchased);
                row.setWarrantyUntil(warranty);
                row.setCommissioned(commissioned);
                row.setSupplier(supplier);
                row.setDocument(document);
                row.setCurrency(currency);
                row.setWarrantyService(service);
                row.setPriceMinor(price.longValue());
                row.setResidualMinor(residual.longValue());
                row.setLifeMonths(months);
                if (draft.getApproverKey() != null && !"pending".equals(row.getWriteoffState())) {
                    String approver = draft.getApproverKey().trim();
                    if (!approver.isEmpty() && userManager().getUserByKey(approver) == null) {
                        throw new AssetException(400, "asset-tree.error.writeoff.approver");
                    }
                    row.setApproverKey(approver);
                }
                row.save();
                return lifecycleDto(asset, row);
            }
        });
    }

    @Override
    public LifecycleDto requestWriteoff(ApplicationUser user, final int assetId, final WriteoffDraft draft) {
        final AssetEntity asset = equipmentForLife(user, assetId, true);
        if (draft == null) {
            throw new AssetException(400, "asset-tree.error.life");
        }
        final String reason = draft.getReason() == null ? "" : draft.getReason().trim();
        final String approver = draft.getApproverKey() == null ? "" : draft.getApproverKey().trim();
        if (reason.isEmpty() || reason.length() > 500) {
            throw new AssetException(400, "asset-tree.error.life");
        }
        if (approver.isEmpty() || userManager().getUserByKey(approver) == null) {
            throw new AssetException(400, "asset-tree.error.writeoff.approver");
        }
        return ao().executeInTransaction(new TransactionCallback<LifecycleDto>() {
            @Override
            public LifecycleDto doInTransaction() {
                LifecycleEntity row = ensureLife(asset.getID());
                String state = row.getWriteoffState() == null ? "" : row.getWriteoffState();
                if ("pending".equals(state) || "approved".equals(state)) {
                    throw new AssetException(400, "asset-tree.error.writeoff");
                }
                row.setWriteoffState("pending");
                row.setWriteoffReason(reason);
                row.setRequesterKey(user.getKey());
                row.setApproverKey(approver);
                row.setDecidedAt("");
                row.save();
                logActivity(asset.getID(), user.getKey(), "writeoff", "", "", reason);
                return lifecycleDto(asset, row);
            }
        });
    }

    @Override
    public LifecycleDto decideWriteoff(ApplicationUser user, final int assetId, final WriteoffDraft draft) {
        final AssetEntity asset = equipmentForLife(user, assetId, false);
        if (draft == null || draft.getApprove() == null) {
            throw new AssetException(400, "asset-tree.error.life");
        }
        final boolean approve = draft.getApprove().booleanValue();
        return ao().executeInTransaction(new TransactionCallback<LifecycleDto>() {
            @Override
            public LifecycleDto doInTransaction() {
                LifecycleEntity row = lifeRow(asset.getID());
                if (row == null || !"pending".equals(row.getWriteoffState())) {
                    throw new AssetException(400, "asset-tree.error.writeoff");
                }
                if (user.getKey() == null || !user.getKey().equals(row.getApproverKey())) {
                    throw new AssetException(403, "asset-tree.error.writeoff.approver");
                }
                row.setWriteoffState(approve ? "approved" : "rejected");
                row.setDecidedAt(LocalDate.now().toString());
                row.save();
                if (approve) {
                    String previous = asset.getStatus();
                    asset.setStatus(Statuses.WRITTEN_OFF);
                    asset.setUpdated(new Date());
                    asset.setUpdatedBy(user.getKey());
                    asset.save();
                    logActivity(asset.getID(), user.getKey(), "status", "", previous, Statuses.WRITTEN_OFF);
                }
                return lifecycleDto(asset, row);
            }
        });
    }

    private AssetEntity equipmentForLife(ApplicationUser user, int assetId, boolean edit) {
        AssetEntity asset = edit
                ? requireAssetCap(user, assetId, GrantCaps.OBJECT)
                : requireAsset(assetId);
        if (!edit) {
            requireReadableProject(user, asset.getProjectKey());
        }
        if (isLocation(asset)) {
            throw new AssetException(400, "asset-tree.error.life");
        }
        requireModule(asset.getProjectKey(), ProjectModules.LIFE);
        return asset;
    }

    private LifecycleEntity lifeRow(int assetId) {
        LifecycleEntity[] rows = ao().find(LifecycleEntity.class, Query.select().where("ASSET_ID = ?", assetId));
        return rows == null || rows.length == 0 ? null : rows[0];
    }

    private LifecycleEntity ensureLife(int assetId) {
        LifecycleEntity row = lifeRow(assetId);
        if (row != null) {
            return row;
        }
        return ao().create(LifecycleEntity.class, new DBParam("ASSET_ID", assetId));
    }

    private LifecycleDto lifecycleDto(AssetEntity asset, LifecycleEntity row) {
        LifecycleDto dto = new LifecycleDto();
        dto.setAssetId(asset.getID());
        LocalDate today = LocalDate.now();
        if (row == null) {
            dto.setPurchased("");
            dto.setSupplier("");
            dto.setPrice("");
            dto.setWarrantyUntil("");
            dto.setResidual("");
            dto.setMonthly("");
            dto.setBook("");
            dto.setWriteoffState("");
            dto.setWriteoffReason("");
            dto.setRequesterKey("");
            dto.setRequesterName("");
            dto.setApproverKey("");
            dto.setApproverName("");
            dto.setDecidedAt("");
            dto.setDocument("");
            dto.setCurrency("RUB");
            dto.setCommissioned("");
            dto.setWarrantyService("");
            return dto;
        }
        dto.setPurchased(row.getPurchased() == null ? "" : row.getPurchased());
        dto.setSupplier(row.getSupplier() == null ? "" : row.getSupplier());
        dto.setPrice(MoneyText.format(row.getPriceMinor()));
        dto.setWarrantyUntil(row.getWarrantyUntil() == null ? "" : row.getWarrantyUntil());
        dto.setLifeMonths(row.getLifeMonths());
        dto.setResidual(MoneyText.format(row.getResidualMinor()));
        dto.setMonthly(MoneyText.format(Depreciation.monthlyMinor(row.getPriceMinor(), row.getResidualMinor(), row.getLifeMonths())));
        dto.setBook(MoneyText.format(bookOf(row, today)));
        String warranty = FieldDates.canonical(row.getWarrantyUntil());
        dto.setWarrantyActive(warranty != null && !warranty.isEmpty() && Depreciation.warrantyActive(LocalDate.parse(warranty), today));
        dto.setWriteoffState(row.getWriteoffState() == null ? "" : row.getWriteoffState());
        dto.setWriteoffReason(row.getWriteoffReason() == null ? "" : row.getWriteoffReason());
        dto.setRequesterKey(row.getRequesterKey() == null ? "" : row.getRequesterKey());
        dto.setRequesterName(displayName(row.getRequesterKey()));
        dto.setApproverKey(row.getApproverKey() == null ? "" : row.getApproverKey());
        dto.setApproverName(displayName(row.getApproverKey()));
        dto.setDecidedAt(row.getDecidedAt() == null ? "" : row.getDecidedAt());
        dto.setDocument(row.getDocument() == null ? "" : row.getDocument());
        dto.setCurrency(row.getCurrency() == null || row.getCurrency().isEmpty() ? "RUB" : row.getCurrency());
        dto.setCommissioned(row.getCommissioned() == null ? "" : row.getCommissioned());
        dto.setWarrantyService(row.getWarrantyService() == null ? "" : row.getWarrantyService());
        return dto;
    }

    private static final class ServiceNotice {
        private final String email;
        private final String subject;
        private final String body;

        private ServiceNotice(String email, String subject, String body) {
            this.email = email;
            this.subject = subject;
            this.body = body;
        }
    }

    private static final Comparator<AssetEntity> SIBLING_ORDER = new Comparator<AssetEntity>() {
        @Override
        public int compare(AssetEntity left, AssetEntity right) {
            int bySort = Integer.compare(left.getSortOrder(), right.getSortOrder());
            return bySort != 0 ? bySort : Integer.compare(left.getID(), right.getID());
        }
    };
}
