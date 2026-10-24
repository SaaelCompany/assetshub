package com.assetstree.jira.service;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.assetstree.jira.dto.AssetDto;
import com.assetstree.jira.dto.AssetKeyDto;
import com.assetstree.jira.dto.FieldOrderDto;
import com.assetstree.jira.dto.ProjectViewDto;
import com.assetstree.jira.dto.BulkResultDto;
import com.assetstree.jira.dto.AssetListDto;
import com.assetstree.jira.dto.AssetTypeDto;
import com.assetstree.jira.dto.CommentDto;
import com.assetstree.jira.dto.FieldDto;
import com.assetstree.jira.dto.FileDto;
import com.assetstree.jira.dto.GrantDto;
import com.assetstree.jira.dto.ImportResultDto;
import com.assetstree.jira.dto.StatusDto;
import com.assetstree.jira.dto.InventoryDto;
import com.assetstree.jira.dto.InventoryRowDto;
import com.assetstree.jira.dto.IssueContextDto;
import com.assetstree.jira.dto.IssueRefDto;
import com.assetstree.jira.dto.MetaDto;
import com.assetstree.jira.dto.PickerNodeDto;
import com.assetstree.jira.dto.FloorDto;
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
import com.assetstree.jira.model.ImportDraft;
import com.assetstree.jira.dto.FlowRuleDto;
import com.assetstree.jira.dto.IssueTypeDto;
import com.assetstree.jira.dto.FlowBookDto;
import com.assetstree.jira.dto.PortalRuleDto;
import com.assetstree.jira.model.MoveDraft;
import com.assetstree.jira.model.PlaceArrangeDraft;
import com.assetstree.jira.model.PlaceTypeDraft;
import com.assetstree.jira.model.FlowRuleDraft;
import com.assetstree.jira.model.PortalRuleDraft;
import com.assetstree.jira.dto.ServicePlanDto;
import com.assetstree.jira.model.LifecycleDraft;
import com.assetstree.jira.model.SavedReportDraft;
import com.assetstree.jira.model.ScheduleDraft;
import com.assetstree.jira.model.StaffDutyDraft;
import com.assetstree.jira.model.ServicePlanDraft;
import com.assetstree.jira.model.WriteoffDraft;
import com.assetstree.jira.model.StatusDraft;
import com.assetstree.jira.model.TypeDraft;

import java.util.List;

public interface AssetService {
    MetaDto meta(ApplicationUser user);

    AssetListDto listAssets(ApplicationUser user, String projectKey, String query);

    AssetDto getAsset(ApplicationUser user, int id);

    AssetDto createAsset(ApplicationUser user, AssetDraft draft);

    AssetDto copyAsset(ApplicationUser user, int id);

    AssetDto updateAsset(ApplicationUser user, int id, AssetDraft draft);

    CommentDto addComment(ApplicationUser user, int id, String body);

    void deleteComment(ApplicationUser user, int assetId, int commentId);

    void deleteActivity(ApplicationUser user, int assetId, int activityId);

    FileDto storeFile(ApplicationUser user, int assetId, String fileName, String contentType, byte[] data);

    StoredFile openFile(ApplicationUser user, int fileId);

    void deleteFile(ApplicationUser user, int assetId, int fileId);

    void storeCardImage(ApplicationUser user, int assetId, byte[] data);

    StoredFile openCardImage(ApplicationUser user, int assetId);

    void deleteCardImage(ApplicationUser user, int assetId);

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

    List<GrantDto> searchGroups(ApplicationUser user, String query);

    IssueRefDto linkIssue(ApplicationUser user, int assetId, String issueKey);

    void unlinkIssue(ApplicationUser user, int assetId, long issueId);

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

    List<AssetDto> assetsForUser(ApplicationUser user, String userKey);

    void syncRequestAsset(ApplicationUser user, Issue issue, String previousAssetId, String nextAssetId);

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

    List<SavedReportDto> reports(ApplicationUser user, String projectKey);

    SavedReportDto createReport(ApplicationUser user, String projectKey, SavedReportDraft draft);

    void deleteReport(ApplicationUser user, String projectKey, String reportKey);

    ReportViewDto reportView(ApplicationUser user, String projectKey, String reportKey);

    LifecycleDto lifecycle(ApplicationUser user, int assetId);

    LifecycleDto saveLifecycle(ApplicationUser user, int assetId, LifecycleDraft draft);

    LifecycleDto requestWriteoff(ApplicationUser user, int assetId, WriteoffDraft draft);

    LifecycleDto decideWriteoff(ApplicationUser user, int assetId, WriteoffDraft draft);
}
