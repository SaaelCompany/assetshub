package com.assetstree.jira.service;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.assetstree.jira.dto.AssetDto;
import com.assetstree.jira.dto.AssetLinkDto;
import com.assetstree.jira.dto.AssetPeerDto;
import com.assetstree.jira.dto.AssetKeyDto;
import com.assetstree.jira.dto.FieldOrderDto;
import com.assetstree.jira.dto.PortalCardDto;
import com.assetstree.jira.dto.PortalMineDto;
import com.assetstree.jira.dto.PortalRequestCreatedDto;
import com.assetstree.jira.dto.PortalRequestFormDto;
import com.assetstree.jira.dto.ProjectViewDto;
import com.assetstree.jira.dto.BulkResultDto;
import com.assetstree.jira.dto.AssetListDto;
import com.assetstree.jira.dto.AssetTypeDto;
import com.assetstree.jira.dto.CommentDto;
import com.assetstree.jira.dto.FieldDto;
import com.assetstree.jira.dto.FileDto;
import com.assetstree.jira.dto.ExportJobDto;
import com.assetstree.jira.dto.ForecastRowDto;
import com.assetstree.jira.dto.GrantDto;
import com.assetstree.jira.dto.SchemaDto;
import com.assetstree.jira.dto.WebhookDto;
import com.assetstree.jira.dto.WebhookProbeDto;
import com.assetstree.jira.dto.ImportResultDto;
import com.assetstree.jira.dto.StatusDto;
import com.assetstree.jira.dto.InventoryDto;
import com.assetstree.jira.dto.InventoryRowDto;
import com.assetstree.jira.dto.IssueContextDto;
import com.assetstree.jira.dto.IssueRefDto;
import com.assetstree.jira.dto.MetaDto;
import com.assetstree.jira.dto.MineViewDto;
import com.assetstree.jira.dto.PickerNodeDto;
import com.assetstree.jira.dto.FloorDto;
import com.assetstree.jira.dto.SchemeDocumentDto;
import com.assetstree.jira.dto.LifecycleDto;
import com.assetstree.jira.dto.ReportDto;
import com.assetstree.jira.dto.ReportViewDto;
import com.assetstree.jira.dto.SavedReportDto;
import com.assetstree.jira.dto.ScheduleRowDto;
import com.assetstree.jira.dto.StaffDutyDto;
import com.assetstree.jira.dto.UserProfileDto;
import com.assetstree.jira.model.AssetDraft;
import com.assetstree.jira.model.CardLayout;
import com.assetstree.jira.model.BulkDraft;
import com.assetstree.jira.model.FieldDraft;
import com.assetstree.jira.model.GrantDraft;
import com.assetstree.jira.model.ExportDraft;
import com.assetstree.jira.model.ImportDraft;
import com.assetstree.jira.model.JsonImportDraft;
import com.assetstree.jira.dto.FlowRuleDto;
import com.assetstree.jira.dto.IssueTypeDto;
import com.assetstree.jira.dto.FlowBookDto;
import com.assetstree.jira.dto.PortalRuleDto;
import com.assetstree.jira.model.MoveDraft;
import com.assetstree.jira.model.PlaceArrangeDraft;
import com.assetstree.jira.model.PlaceTypeDraft;
import com.assetstree.jira.model.FlowRuleDraft;
import com.assetstree.jira.model.PortalRequestDraft;
import com.assetstree.jira.model.PortalRuleDraft;
import com.assetstree.jira.dto.ServicePlanDto;
import com.assetstree.jira.model.LifecycleDraft;
import com.assetstree.jira.model.SavedReportDraft;
import com.assetstree.jira.model.ScheduleDraft;
import com.assetstree.jira.model.StaffDutyDraft;
import com.assetstree.jira.model.ServicePlanDraft;
import com.assetstree.jira.model.WriteoffDraft;
import com.assetstree.jira.model.StatusDraft;
import com.assetstree.jira.dto.TniSourceDto;
import com.assetstree.jira.dto.TniWatchDto;
import com.assetstree.jira.dto.TniSubnetDto;
import com.assetstree.jira.model.TniSheetDraft;
import com.assetstree.jira.model.TniSightDraft;
import com.assetstree.jira.model.TniSourceDraft;
import com.assetstree.jira.dto.TniWatchDraft;
import com.assetstree.jira.model.TniSubnetDraft;
import com.assetstree.jira.model.TypeDraft;

import java.util.List;

public interface AssetService {
    MetaDto meta(ApplicationUser user);

    MineViewDto saveMineView(ApplicationUser user, MineViewDto draft);

    AssetListDto listAssets(ApplicationUser user, String projectKey, String query);

    String treeMark(ApplicationUser user, String projectKey);

    AssetDto getAsset(ApplicationUser user, int id);

    AssetDto createAsset(ApplicationUser user, AssetDraft draft);

    AssetDto copyAsset(ApplicationUser user, int id);

    AssetDto updateAsset(ApplicationUser user, int id, AssetDraft draft);

    CommentDto addComment(ApplicationUser user, int id, String body, boolean internal);

    void deleteComment(ApplicationUser user, int assetId, int commentId);

    void deleteActivity(ApplicationUser user, int assetId, int activityId);

    FileDto storeFile(ApplicationUser user, int assetId, String fileName, String contentType, byte[] data, boolean internal);

    FileDto storeFile(ApplicationUser user, int assetId, String fileName, String contentType, byte[] data, boolean internal, String slot);

    StoredFile openFile(ApplicationUser user, int fileId);

    void deleteFile(ApplicationUser user, int assetId, int fileId);

    UserProfileDto shareAsset(ApplicationUser user, int assetId, String userKey);

    void unshareAsset(ApplicationUser user, int assetId, String userKey);

    PortalCardDto portalAsset(ApplicationUser user, int id);

    CommentDto portalComment(ApplicationUser user, int id, String body, boolean internal);

    FileDto portalStoreFile(ApplicationUser user, int assetId, String fileName, String contentType, byte[] data, boolean internal);

    void portalDeleteFile(ApplicationUser user, int assetId, int fileId);

    UserProfileDto portalShare(ApplicationUser user, int assetId, String userKey);

    void portalUnshare(ApplicationUser user, int assetId, String userKey);

    void storeCardImage(ApplicationUser user, int assetId, byte[] data);

    StoredFile openCardImage(ApplicationUser user, int assetId);

    void deleteCardImage(ApplicationUser user, int assetId);

    void storePhoto(ApplicationUser user, int assetId, byte[] data);

    StoredFile openPhoto(ApplicationUser user, int assetId);

    void deletePhoto(ApplicationUser user, int assetId);

    void deleteAsset(ApplicationUser user, int id, boolean cascade);

    AssetDto moveAsset(ApplicationUser user, int id, MoveDraft move);

    AssetTypeDto createType(ApplicationUser user, TypeDraft draft);

    AssetTypeDto updateType(ApplicationUser user, String typeKey, TypeDraft draft);

    void deleteType(ApplicationUser user, String typeKey);

    FieldDto addField(ApplicationUser user, String typeKey, FieldDraft draft);

    FieldDto updateField(ApplicationUser user, String typeKey, String fieldKey, FieldDraft draft);

    void deleteField(ApplicationUser user, String typeKey, String fieldKey);

    List<FieldDto> reorderFields(ApplicationUser user, String typeKey, FieldOrderDto order);

    List<StatusDto> listStatuses(ApplicationUser user, String projectKey);

    StatusDto createStatus(ApplicationUser user, String projectKey, StatusDraft draft);

    StatusDto updateStatus(ApplicationUser user, String projectKey, String statusKey, StatusDraft draft);

    void deleteStatus(ApplicationUser user, String projectKey, String statusKey);

    List<GrantDto> listGrants(ApplicationUser user, String projectKey);

    GrantDto addGrant(ApplicationUser user, String projectKey, GrantDraft draft);

    void deleteGrant(ApplicationUser user, String projectKey, String groupName);

    void deleteUserGrant(ApplicationUser user, String projectKey, String userKey);

    List<GrantDto> searchGroups(ApplicationUser user, String query);

    IssueRefDto linkIssue(ApplicationUser user, int assetId, String issueKey);

    void unlinkIssue(ApplicationUser user, int assetId, long issueId);

    AssetPeerDto linkPeer(ApplicationUser user, int assetId, int otherId);

    void unlinkPeer(ApplicationUser user, int assetId, int otherId);

    AssetLinkDto addLink(ApplicationUser user, int assetId, com.assetstree.jira.model.LinkDraft draft);

    AssetLinkDto updateLink(ApplicationUser user, int assetId, int linkId, com.assetstree.jira.model.LinkDraft draft);

    void deleteLink(ApplicationUser user, int assetId, int linkId);

    List<AssetPeerDto> searchPeers(ApplicationUser user, int assetId, String query);

    List<AssetDto> assetsForIssue(ApplicationUser user, long issueId);

    AssetDto linkAssetToIssue(ApplicationUser user, long issueId, int assetId);

    List<AssetDto> searchForIssue(ApplicationUser user, long issueId, String query);

    IssueContextDto issueContext(ApplicationUser user, long issueId);

    ReportDto report(ApplicationUser user, String projectKey);

    byte[] exportEquipment(ApplicationUser user, String projectKey);

    ImportResultDto importEquipment(ApplicationUser user, String projectKey, ImportDraft draft);

    BulkResultDto applyBulk(ApplicationUser user, String projectKey, BulkDraft draft);

    InventoryDto inventory(ApplicationUser user, String projectKey);

    InventoryRowDto markInventory(ApplicationUser user, int id, boolean checked);

    List<PickerNodeDto> picker(ApplicationUser user, String projectKey);

    List<PortalRuleDto> listPortalRules(ApplicationUser user, String projectKey);

    PortalRuleDto addPortalRule(ApplicationUser user, String projectKey, PortalRuleDraft draft);

    PortalRuleDto updatePortalRule(ApplicationUser user, String projectKey, int ruleId, PortalRuleDraft draft);

    void deletePortalRule(ApplicationUser user, String projectKey, int ruleId);

    List<IssueTypeDto> listIssueTypes(ApplicationUser user, String projectKey);

    List<IssueTypeDto> listIssueStatuses(ApplicationUser user, String projectKey);

    List<FlowRuleDto> listFlowRules(ApplicationUser user, String projectKey);

    FlowRuleDto saveFlowRule(ApplicationUser user, String projectKey, FlowRuleDraft draft);

    void deleteFlowRule(ApplicationUser user, String projectKey, int ruleId);

    FlowBookDto flowGraph(ApplicationUser user, String projectKey);

    FlowBookDto saveFlowGraph(ApplicationUser user, String projectKey, FlowBookDto draft);

    CardLayout cardLayout(ApplicationUser user, String projectKey);

    CardLayout saveCardLayout(ApplicationUser user, String projectKey, CardLayout draft);

    /** Stores the prefix used by future object keys. Keys already issued stay unchanged. */
    AssetKeyDto saveAssetKey(ApplicationUser user, String projectKey, AssetKeyDto draft);

    ProjectViewDto saveProjectView(ApplicationUser user, String projectKey, ProjectViewDto draft);

    List<UserProfileDto> searchUsers(ApplicationUser user, String query);

    UserProfileDto userProfile(ApplicationUser user, String userKey);

    List<AssetDto> assetsForUser(ApplicationUser user, String userKey);

    /** Equipment the signed-in customer is responsible for, for the portal profile. */
    PortalMineDto portalMine(ApplicationUser user);

    /** The portal card layout used by every project. Only an administrator can change it. */
    String savePortalLayout(ApplicationUser user, String projectKey, String layout);

    /** Request types the customer can raise for one piece of their equipment. */
    PortalRequestFormDto portalRequestForm(ApplicationUser user, int assetId);

    /** Creates a customer request and attaches the equipment that was open. */
    PortalRequestCreatedDto createPortalRequest(ApplicationUser user, int assetId, PortalRequestDraft draft);

    /**
     * Attaches equipment to a request the customer just raised.
     * Used when the portal form has no asset field to carry the id.
     */
    void portalLinkRequest(ApplicationUser user, int assetId, String issueKey);

    /** Remembers the equipment chosen in the profile until the portal creates the request. */
    void rememberPortalAsset(ApplicationUser user, int assetId);

    /** True when this asset was already attached to the request opened from the profile. */
    boolean portalAssetSettled(ApplicationUser user, int assetId);

    /** Links that equipment to a request the same customer just raised, even when the request type has no asset field. */
    void attachPortalAsset(ApplicationUser user, Issue issue);

    void syncRequestAsset(ApplicationUser user, Issue issue, String previousAssetId, String nextAssetId);

    /**
     * The asset stored in the request field is the same object as the link on the request.
     * A save that only wrote the field still gains the link, including after the request is opened.
     */
    void ensureIssueFieldLink(ApplicationUser user, Issue issue);

    /**
     * The asset chosen in the request form. The text may be the numeric id,
     * the object key, or the line shown in the list, such as a place and a name.
     */
    Integer resolvePortalAsset(String projectKey, String text, String context);

    /** Moves equipment already linked to this request when the request itself changes status. */
    void syncRequestStatus(ApplicationUser user, Issue issue);

    String describeAsset(String assetId);

    ServicePlanDto createPlan(ApplicationUser user, int assetId, ServicePlanDraft draft);

    /** Pins an equipment type to one place, or removes that pin when the place has no such equipment. */
    AssetDto offerType(ApplicationUser user, int assetId, PlaceTypeDraft draft, boolean present);

    /** Moves a type up, down, into the type above, or back onto the place. */
    AssetDto arrangeType(ApplicationUser user, int assetId, PlaceArrangeDraft draft);

    ServicePlanDto updatePlan(ApplicationUser user, int assetId, int planId, ServicePlanDraft draft);

    void deletePlan(ApplicationUser user, int assetId, int planId);

    /** Moves equipment whose service date has arrived and queues the letters. */
    void applyDuePlans();

    List<ScheduleRowDto> schedule(ApplicationUser user, String projectKey);

    List<ScheduleRowDto> createSchedule(ApplicationUser user, String projectKey, ScheduleDraft draft);

    List<StaffDutyDto> staff(ApplicationUser user, String projectKey);

    StaffDutyDto saveStaff(ApplicationUser user, String projectKey, StaffDutyDraft draft);

    StaffDutyDto addStaffLeave(ApplicationUser user, String projectKey, StaffDutyDraft draft);

    void deleteStaff(ApplicationUser user, String projectKey, int dutyId);

    List<FloorDto> floors(ApplicationUser user, String projectKey);

    FloorDto createFloor(ApplicationUser user, String projectKey, FloorDto draft);

    FloorDto saveFloor(ApplicationUser user, String projectKey, int floorId, FloorDto draft);

    void deleteFloor(ApplicationUser user, String projectKey, int floorId);

    List<SchemeDocumentDto> schemes(ApplicationUser user, String projectKey);

    SchemeDocumentDto scheme(ApplicationUser user, String projectKey, int schemeId);

    SchemeDocumentDto createScheme(ApplicationUser user, String projectKey, SchemeDocumentDto draft);

    SchemeDocumentDto saveScheme(ApplicationUser user, String projectKey, int schemeId, SchemeDocumentDto draft);

    SchemeDocumentDto shareScheme(ApplicationUser user, String projectKey, int schemeId, SchemeDocumentDto draft);

    void deleteScheme(ApplicationUser user, String projectKey, int schemeId);

    List<SavedReportDto> reports(ApplicationUser user, String projectKey);

    SavedReportDto createReport(ApplicationUser user, String projectKey, SavedReportDraft draft);

    SavedReportDto updateReportChart(ApplicationUser user, String projectKey, String reportKey, String chart);

    SavedReportDto updateSavedReport(ApplicationUser user, String projectKey, String reportKey, SavedReportDraft draft);

    void deleteReport(ApplicationUser user, String projectKey, String reportKey);

    ReportViewDto reportView(ApplicationUser user, String projectKey, String reportKey, int lineFrom, boolean allLines, String bucket);

    byte[] exportReports(ApplicationUser user, String projectKey);

    byte[] exportReport(ApplicationUser user, String projectKey, String reportKey, String chart);

    void sendReportMail(ApplicationUser user, String projectKey);

    void sendScheduledReports();

    LifecycleDto lifecycle(ApplicationUser user, int assetId);

    LifecycleDto saveLifecycle(ApplicationUser user, int assetId, LifecycleDraft draft);

    LifecycleDto requestWriteoff(ApplicationUser user, int assetId, WriteoffDraft draft);

    LifecycleDto decideWriteoff(ApplicationUser user, int assetId, WriteoffDraft draft);

    ImportResultDto importJson(ApplicationUser user, String projectKey, JsonImportDraft draft);

    AssetDto patchByKey(ApplicationUser user, String projectKey, String objectKey, AssetDraft draft, boolean skipBlank);

    List<SchemaDto> listSchemas(ApplicationUser user, String projectKey);

    SchemaDto saveSchema(ApplicationUser user, String projectKey, SchemaDto draft);

    void deleteSchema(ApplicationUser user, String projectKey, int schemaId);

    List<WebhookDto> listWebhooks(ApplicationUser user, String projectKey);

    WebhookDto saveWebhook(ApplicationUser user, String projectKey, WebhookDto draft);

    WebhookProbeDto probeWebhook(ApplicationUser user, String projectKey, WebhookDto draft);

    void deleteWebhook(ApplicationUser user, String projectKey, int webhookId);

    ExportJobDto startExport(ApplicationUser user, String projectKey, ExportDraft draft);

    ExportJobDto exportStatus(ApplicationUser user, String projectKey, String jobId);

    byte[] exportFile(ApplicationUser user, String projectKey, String jobId);

    String exportFileName(ApplicationUser user, String projectKey, String jobId);

    List<ForecastRowDto> forecast(ApplicationUser user, String projectKey, int within);

    /** Stores a browser report from the portal create form. The asset is created when the request exists. */
    void rememberTni(ApplicationUser user, TniSightDraft draft);

    /** Links the reporter's latest portal device to a new request and creates the asset. */
    void attachTni(ApplicationUser user, Issue issue);

    /** Pings stored devices whose storage period has ended and retires the ones that stay silent. */
    void applyTniRetention();

    List<TniSubnetDto> tniSubnets(ApplicationUser user, String projectKey);

    TniSubnetDto createTniSubnet(ApplicationUser user, String projectKey, TniSubnetDraft draft);

    void deleteTniSubnet(ApplicationUser user, String projectKey, int subnetId);

    TniSubnetDto scanTniSubnet(ApplicationUser user, String projectKey, int subnetId);

    TniSourceDto tniDirectory(ApplicationUser user, String projectKey);

    TniSourceDto saveTniDirectory(ApplicationUser user, String projectKey, TniSourceDraft draft);

    ImportResultDto loadTniDirectory(ApplicationUser user, String projectKey, TniSourceDraft draft);

    TniSourceDto tniSccm(ApplicationUser user, String projectKey);

    TniSourceDto saveTniSccm(ApplicationUser user, String projectKey, TniSourceDraft draft);

    ImportResultDto loadTniSccm(ApplicationUser user, String projectKey, TniSourceDraft draft);

    TniWatchDto tniWatch(ApplicationUser user, int assetId);

    TniWatchDto saveTniWatch(ApplicationUser user, int assetId, TniWatchDraft draft);

    TniWatchDto pingTni(ApplicationUser user, int assetId);

    ImportResultDto importTniSheet(ApplicationUser user, String projectKey, TniSheetDraft draft);
}
