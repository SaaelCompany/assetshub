package com.assetstree.jira.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.assetstree.jira.dto.AssetKeyDto;
import com.assetstree.jira.dto.FieldOrderDto;
import com.assetstree.jira.dto.ProjectViewDto;
import com.assetstree.jira.dto.MessageDto;
import com.assetstree.jira.dto.MineViewDto;
import com.assetstree.jira.model.AssetDraft;
import com.assetstree.jira.model.CardLayout;
import com.assetstree.jira.model.BulkDraft;
import com.assetstree.jira.model.CommentDraft;
import com.assetstree.jira.model.ShareDraft;
import com.assetstree.jira.model.AssetException;
import com.assetstree.jira.model.FieldDraft;
import com.assetstree.jira.model.GrantDraft;
import com.assetstree.jira.dto.FloorDto;
import com.assetstree.jira.dto.SchemeDocumentDto;
import com.assetstree.jira.dto.SchemaDto;
import com.assetstree.jira.dto.WebhookDto;
import com.assetstree.jira.model.ExportDraft;
import com.assetstree.jira.model.ImportDraft;
import com.assetstree.jira.model.JsonImportDraft;
import com.assetstree.jira.model.InventoryDraft;
import com.assetstree.jira.model.IssueLinkDraft;
import com.assetstree.jira.model.LinkDraft;
import com.assetstree.jira.model.MoveDraft;
import com.assetstree.jira.dto.PortalFieldsDto;
import com.assetstree.jira.dto.PortalProjectDto;
import com.assetstree.jira.dto.FlowBookDto;
import com.assetstree.jira.model.FlowRuleDraft;
import com.assetstree.jira.model.PortalRequestDraft;
import com.assetstree.jira.model.PortalRuleDraft;
import com.assetstree.jira.model.PlaceArrangeDraft;
import com.assetstree.jira.model.PlaceTypeDraft;
import com.assetstree.jira.model.LifecycleDraft;
import com.assetstree.jira.model.SavedReportDraft;
import com.assetstree.jira.model.ScheduleDraft;
import com.assetstree.jira.model.StaffDutyDraft;
import com.assetstree.jira.model.ServicePlanDraft;
import com.assetstree.jira.model.WriteoffDraft;
import com.assetstree.jira.model.StatusDraft;
import com.assetstree.jira.web.PortalLookup;
import com.assetstree.jira.model.TypeDraft;
import com.assetstree.jira.model.TniSightDraft;
import com.assetstree.jira.model.TniSourceDraft;
import com.assetstree.jira.dto.TniWatchDraft;
import com.assetstree.jira.model.TniSubnetDraft;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.Callable;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AssetResource {
    private static final Logger log = LoggerFactory.getLogger(AssetResource.class);

    private final AssetService assetService;

    @Context
    private HttpServletRequest request;

    public AssetResource() {
        this.assetService = AssetBridge.service();
    }

    @GET
    @Path("meta")
    public Response meta() {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.meta(user())).build();
            }
        });
    }

    @PUT
    @Path("me/columns")
    public Response saveMine(final MineViewDto draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveMineView(user(), draft)).build();
            }
        });
    }

    @GET
    @Path("assets")
    public Response list(@QueryParam("projectKey") final String projectKey, @QueryParam("q") final String query) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.listAssets(user(), projectKey, query)).build();
            }
        });
    }

    @POST
    @Path("assets")
    public Response create(final AssetDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.createAsset(user(), draft)).build();
            }
        });
    }

    @GET
    @Path("assets/{id}")
    public Response get(@PathParam("id") final int id) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.getAsset(user(), id)).build();
            }
        });
    }

    @PUT
    @Path("assets/{id}")
    public Response update(@PathParam("id") final int id, final AssetDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.updateAsset(user(), id, draft)).build();
            }
        });
    }

    @POST
    @Path("assets/{id}/types/arrange")
    public Response arrangeType(@PathParam("id") final int id, final PlaceArrangeDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.arrangeType(user(), id, draft)).build();
            }
        });
    }

    @POST
    @Path("assets/{id}/types")
    public Response offerType(@PathParam("id") final int id, final PlaceTypeDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.offerType(user(), id, draft, true)).build();
            }
        });
    }

    @DELETE
    @Path("assets/{id}/types/{typeKey}")
    public Response withdrawType(@PathParam("id") final int id, @PathParam("typeKey") final String typeKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                PlaceTypeDraft draft = new PlaceTypeDraft();
                draft.setTypeKey(typeKey);
                assetService.offerType(user(), id, draft, false);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("assets/{id}/plans")
    public Response createPlan(@PathParam("id") final int id, final ServicePlanDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.createPlan(user(), id, draft)).build();
            }
        });
    }

    @PUT
    @Path("assets/{id}/plans/{planId}")
    public Response updatePlan(@PathParam("id") final int id, @PathParam("planId") final int planId, final ServicePlanDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.updatePlan(user(), id, planId, draft)).build();
            }
        });
    }

    @DELETE
    @Path("assets/{id}/plans/{planId}")
    public Response deletePlan(@PathParam("id") final int id, @PathParam("planId") final int planId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deletePlan(user(), id, planId);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("assets/{id}/comments")
    public Response addComment(@PathParam("id") final int id, final CommentDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                String body = draft == null ? null : draft.getBody();
                boolean internal = draft != null && draft.isInternal();
                return Response.status(Response.Status.CREATED).entity(assetService.addComment(user(), id, body, internal)).build();
            }
        });
    }

    @DELETE
    @Path("assets/{id}/comments/{commentId}")
    public Response deleteComment(@PathParam("id") final int id, @PathParam("commentId") final int commentId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteComment(user(), id, commentId);
                return Response.noContent().build();
            }
        });
    }

    @DELETE
    @Path("assets/{id}/activities/{activityId}")
    public Response deleteActivity(@PathParam("id") final int id, @PathParam("activityId") final int activityId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteActivity(user(), id, activityId);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("assets/{id}/shares")
    public Response share(@PathParam("id") final int id, final ShareDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                String userKey = draft == null ? null : draft.getUserKey();
                return Response.status(Response.Status.CREATED).entity(assetService.shareAsset(user(), id, userKey)).build();
            }
        });
    }

    @DELETE
    @Path("assets/{id}/shares/{userKey}")
    public Response unshare(@PathParam("id") final int id, @PathParam("userKey") final String userKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.unshareAsset(user(), id, userKey);
                return Response.noContent().build();
            }
        });
    }

    @DELETE
    @Path("assets/{id}/files/{fileId}")
    public Response deleteFile(@PathParam("id") final int id, @PathParam("fileId") final int fileId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteFile(user(), id, fileId);
                return Response.noContent().build();
            }
        });
    }

    @DELETE
    @Path("assets/{id}")
    public Response delete(@PathParam("id") final int id, @QueryParam("cascade") final boolean cascade) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteAsset(user(), id, cascade);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("assets/{id}/inventory")
    public Response inventoryMark(@PathParam("id") final int id, final InventoryDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                boolean checked = draft != null && draft.isChecked();
                return Response.ok(assetService.markInventory(user(), id, checked)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/inventory")
    public Response inventory(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.inventory(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("assets/{id}/copy")
    public Response copy(@PathParam("id") final int id) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.copyAsset(user(), id)).build();
            }
        });
    }

    @POST
    @Path("assets/{id}/move")
    public Response move(@PathParam("id") final int id, final MoveDraft move) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.moveAsset(user(), id, move)).build();
            }
        });
    }

    @POST
    @Path("assets/{id}/issues")
    public Response linkIssue(@PathParam("id") final int id, final IssueLinkDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                String issueKey = draft == null ? null : draft.getIssueKey();
                return Response.status(Response.Status.CREATED).entity(assetService.linkIssue(user(), id, issueKey)).build();
            }
        });
    }

    @DELETE
    @Path("assets/{id}/issues/{issueId}")
    public Response unlinkIssue(@PathParam("id") final int id, @PathParam("issueId") final long issueId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.unlinkIssue(user(), id, issueId);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("assets/{id}/peers")
    public Response linkPeer(@PathParam("id") final int id, final IssueLinkDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                Integer otherId = draft == null ? null : draft.getAssetId();
                if (otherId == null) {
                    throw new AssetException(400, "asset-tree.error.peer.notFound");
                }
                return Response.status(Response.Status.CREATED)
                        .entity(assetService.linkPeer(user(), id, otherId.intValue()))
                        .build();
            }
        });
    }

    @DELETE
    @Path("assets/{id}/peers/{otherId}")
    public Response unlinkPeer(@PathParam("id") final int id, @PathParam("otherId") final int otherId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.unlinkPeer(user(), id, otherId);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("assets/{id}/links")
    public Response addLink(@PathParam("id") final int id, final LinkDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.addLink(user(), id, draft)).build();
            }
        });
    }

    @PUT
    @Path("assets/{id}/links/{linkId}")
    public Response updateLink(@PathParam("id") final int id, @PathParam("linkId") final int linkId, final LinkDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.updateLink(user(), id, linkId, draft)).build();
            }
        });
    }

    @DELETE
    @Path("assets/{id}/links/{linkId}")
    public Response deleteLink(@PathParam("id") final int id, @PathParam("linkId") final int linkId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteLink(user(), id, linkId);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("assets/{id}/peers/search")
    public Response searchPeers(@PathParam("id") final int id, @QueryParam("q") final String query) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.searchPeers(user(), id, query)).build();
            }
        });
    }

    @GET
    @Path("issues/{issueId}/assets")
    public Response assetsForIssue(@PathParam("issueId") final long issueId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.assetsForIssue(user(), issueId)).build();
            }
        });
    }

    @POST
    @Path("issues/{issueId}/assets")
    public Response linkAsset(@PathParam("issueId") final long issueId, final IssueLinkDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                Integer assetId = draft == null ? null : draft.getAssetId();
                if (assetId == null) {
                    throw new AssetException(404, "asset-tree.error.notFound");
                }
                return Response.status(Response.Status.CREATED)
                        .entity(assetService.linkAssetToIssue(user(), issueId, assetId.intValue()))
                        .build();
            }
        });
    }

    @POST
    @Path("types")
    public Response createType(final TypeDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.createType(user(), draft)).build();
            }
        });
    }

    @POST
    @Path("types/{typeKey}/fields")
    public Response addField(@PathParam("typeKey") final String typeKey, final FieldDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.addField(user(), typeKey, draft)).build();
            }
        });
    }

    @PUT
    @Path("types/{typeKey}/fields/{fieldKey}")
    public Response updateField(@PathParam("typeKey") final String typeKey, @PathParam("fieldKey") final String fieldKey,
                                final FieldDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.updateField(user(), typeKey, fieldKey, draft)).build();
            }
        });
    }

    @PUT
    @Path("types/{typeKey}/field-order")
    public Response reorderFields(@PathParam("typeKey") final String typeKey, final FieldOrderDto order) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.reorderFields(user(), typeKey, order)).build();
            }
        });
    }

    @DELETE
    @Path("types/{typeKey}/fields/{fieldKey}")
    public Response deleteField(@PathParam("typeKey") final String typeKey, @PathParam("fieldKey") final String fieldKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteField(user(), typeKey, fieldKey);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/report")
    public Response report(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.report(user(), projectKey)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/schedule")
    public Response schedule(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.schedule(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/schedule")
    public Response createSchedule(@PathParam("projectKey") final String projectKey, final ScheduleDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.createSchedule(user(), projectKey, draft)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/staff")
    public Response staff(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.staff(user(), projectKey)).build();
            }
        });
    }

    @PUT
    @Path("projects/{projectKey}/staff")
    public Response saveStaff(@PathParam("projectKey") final String projectKey, final StaffDutyDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveStaff(user(), projectKey, draft)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/staff/leave")
    public Response addStaffLeave(@PathParam("projectKey") final String projectKey, final StaffDutyDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.addStaffLeave(user(), projectKey, draft)).build();
            }
        });
    }

    @DELETE
    @Path("projects/{projectKey}/staff/{dutyId}")
    public Response deleteStaff(@PathParam("projectKey") final String projectKey, @PathParam("dutyId") final int dutyId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteStaff(user(), projectKey, dutyId);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/floors")
    public Response floors(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.floors(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/floors")
    public Response createFloor(@PathParam("projectKey") final String projectKey, final FloorDto draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.createFloor(user(), projectKey, draft)).build();
            }
        });
    }

    @PUT
    @Path("projects/{projectKey}/floors/{floorId}")
    public Response saveFloor(@PathParam("projectKey") final String projectKey, @PathParam("floorId") final int floorId, final FloorDto draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveFloor(user(), projectKey, floorId, draft)).build();
            }
        });
    }

    @DELETE
    @Path("projects/{projectKey}/floors/{floorId}")
    public Response deleteFloor(@PathParam("projectKey") final String projectKey, @PathParam("floorId") final int floorId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteFloor(user(), projectKey, floorId);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/schemes")
    public Response schemes(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.schemes(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/schemes")
    public Response createScheme(@PathParam("projectKey") final String projectKey, final SchemeDocumentDto draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.createScheme(user(), projectKey, draft)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/schemes/{schemeId}")
    public Response scheme(@PathParam("projectKey") final String projectKey, @PathParam("schemeId") final int schemeId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.scheme(user(), projectKey, schemeId)).build();
            }
        });
    }

    @PUT
    @Path("projects/{projectKey}/schemes/{schemeId}")
    public Response saveScheme(@PathParam("projectKey") final String projectKey, @PathParam("schemeId") final int schemeId, final SchemeDocumentDto draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveScheme(user(), projectKey, schemeId, draft)).build();
            }
        });
    }

    @DELETE
    @Path("projects/{projectKey}/schemes/{schemeId}")
    public Response deleteScheme(@PathParam("projectKey") final String projectKey, @PathParam("schemeId") final int schemeId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteScheme(user(), projectKey, schemeId);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/reports")
    public Response reports(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.reports(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/reports")
    public Response createReport(@PathParam("projectKey") final String projectKey, final SavedReportDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.createReport(user(), projectKey, draft)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/reports/{reportKey}/book.xlsx")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response exportReport(@PathParam("projectKey") final String projectKey, @PathParam("reportKey") final String reportKey,
            @QueryParam("chart") final String chart) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                byte[] book = assetService.exportReport(user(), projectKey, reportKey, chart);
                String file = "report-" + reportKey + ".xlsx";
                return Response.ok(book, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .header("Content-Disposition", "attachment; filename=\"" + file + "\"")
                        .build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/reports.xlsx")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response exportReports(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                byte[] book = assetService.exportReports(user(), projectKey);
                String file = "reports-" + projectKey + ".xlsx";
                return Response.ok(book, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .header("Content-Disposition", "attachment; filename=\"" + file + "\"")
                        .build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/reports/mail")
    public Response sendReportMail(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.sendReportMail(user(), projectKey);
                return Response.noContent().build();
            }
        });
    }

    @PUT
    @Path("projects/{projectKey}/reports/{reportKey}")
    public Response updateReportChart(@PathParam("projectKey") final String projectKey, @PathParam("reportKey") final String reportKey, final SavedReportDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                boolean editing = draft != null && ((draft.getName() != null && !draft.getName().trim().isEmpty())
                        || (draft.getGroupBy() != null && !draft.getGroupBy().trim().isEmpty()));
                if (editing) {
                    return Response.ok(assetService.updateSavedReport(user(), projectKey, reportKey, draft)).build();
                }
                String chart = draft == null ? "" : draft.getChart();
                return Response.ok(assetService.updateReportChart(user(), projectKey, reportKey, chart)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/reports/{reportKey}")
    public Response reportView(@PathParam("projectKey") final String projectKey, @PathParam("reportKey") final String reportKey,
            @QueryParam("from") final Integer from, @QueryParam("lines") final String lines, @QueryParam("bucket") final String bucket) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                int lineFrom = from == null ? 0 : from.intValue();
                boolean allLines = lines != null && "all".equalsIgnoreCase(lines.trim());
                return Response.ok(assetService.reportView(user(), projectKey, reportKey, lineFrom, allLines, bucket)).build();
            }
        });
    }

    @DELETE
    @Path("projects/{projectKey}/reports/{reportKey}")
    public Response deleteReport(@PathParam("projectKey") final String projectKey, @PathParam("reportKey") final String reportKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteReport(user(), projectKey, reportKey);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("assets/{id}/lifecycle")
    public Response lifecycle(@PathParam("id") final int id) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.lifecycle(user(), id)).build();
            }
        });
    }

    @PUT
    @Path("assets/{id}/lifecycle")
    public Response saveLifecycle(@PathParam("id") final int id, final LifecycleDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveLifecycle(user(), id, draft)).build();
            }
        });
    }

    @POST
    @Path("assets/{id}/lifecycle/writeoff")
    public Response requestWriteoff(@PathParam("id") final int id, final WriteoffDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.requestWriteoff(user(), id, draft)).build();
            }
        });
    }

    @POST
    @Path("assets/{id}/lifecycle/decision")
    public Response decideWriteoff(@PathParam("id") final int id, final WriteoffDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.decideWriteoff(user(), id, draft)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/equipment.xlsx")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response exportEquipment(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                byte[] book = assetService.exportEquipment(user(), projectKey);
                String file = "equipment-" + projectKey + ".xlsx";
                return Response.ok(book, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .header("Content-Disposition", "attachment; filename=\"" + file + "\"")
                        .build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/bulk")
    public Response bulk(@PathParam("projectKey") final String projectKey, final BulkDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.applyBulk(user(), projectKey, draft)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/equipment")
    public Response importEquipment(@PathParam("projectKey") final String projectKey, final ImportDraft draft,
                                    @HeaderParam("X-Idempotency-Key") final String idempotencyKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                applyIdempotency(draft, idempotencyKey);
                return Response.ok(assetService.importEquipment(user(), projectKey, draft)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/import")
    public Response importJson(@PathParam("projectKey") final String projectKey, final JsonImportDraft draft,
                               @HeaderParam("X-Idempotency-Key") final String idempotencyKey) {
        return receiveJson(projectKey, draft, idempotencyKey);
    }

    @POST
    @Path("projects/{projectKey}/discovery")
    public Response discovery(@PathParam("projectKey") final String projectKey, final JsonImportDraft draft,
                              @HeaderParam("X-Idempotency-Key") final String idempotencyKey) {
        return receiveJson(projectKey, draft, idempotencyKey);
    }

    @PUT
    @Path("projects/{projectKey}/objects/{objectKey}")
    public Response patchObject(@PathParam("projectKey") final String projectKey,
                                @PathParam("objectKey") final String objectKey,
                                final AssetDraft draft,
                                @QueryParam("skipBlank") final Boolean skipBlank) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                boolean skip = skipBlank == null || skipBlank.booleanValue();
                return Response.ok(assetService.patchByKey(user(), projectKey, objectKey, draft, skip)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/schemas")
    public Response schemas(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.listSchemas(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/schemas")
    public Response saveSchema(@PathParam("projectKey") final String projectKey, final SchemaDto draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveSchema(user(), projectKey, draft)).build();
            }
        });
    }

    @DELETE
    @Path("projects/{projectKey}/schemas/{schemaId}")
    public Response deleteSchema(@PathParam("projectKey") final String projectKey, @PathParam("schemaId") final int schemaId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteSchema(user(), projectKey, schemaId);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/webhooks")
    public Response webhooks(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.listWebhooks(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/webhooks")
    public Response saveWebhook(@PathParam("projectKey") final String projectKey, final WebhookDto draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveWebhook(user(), projectKey, draft)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/webhooks/test")
    public Response probeWebhook(@PathParam("projectKey") final String projectKey, final WebhookDto draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.probeWebhook(user(), projectKey, draft)).build();
            }
        });
    }

    @DELETE
    @Path("projects/{projectKey}/webhooks/{webhookId}")
    public Response deleteWebhook(@PathParam("projectKey") final String projectKey, @PathParam("webhookId") final int webhookId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteWebhook(user(), projectKey, webhookId);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/exports")
    public Response startExport(@PathParam("projectKey") final String projectKey, final ExportDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.startExport(user(), projectKey, draft)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/exports/{jobId}")
    public Response exportStatus(@PathParam("projectKey") final String projectKey, @PathParam("jobId") final String jobId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.exportStatus(user(), projectKey, jobId)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/exports/{jobId}/file")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response exportFile(@PathParam("projectKey") final String projectKey, @PathParam("jobId") final String jobId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                byte[] book = assetService.exportFile(user(), projectKey, jobId);
                String file = assetService.exportFileName(user(), projectKey, jobId);
                return Response.ok(book, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        .header("Content-Disposition", "attachment; filename=\"" + file + "\"")
                        .build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/lifecycle/forecast")
    public Response forecast(@PathParam("projectKey") final String projectKey, @QueryParam("within") final Integer within) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                int window = within == null ? 12 : within.intValue();
                return Response.ok(assetService.forecast(user(), projectKey, window)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/statuses")
    public Response statuses(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.listStatuses(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/statuses")
    public Response createStatus(@PathParam("projectKey") final String projectKey, final StatusDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED)
                        .entity(assetService.createStatus(user(), projectKey, draft))
                        .build();
            }
        });
    }

    @PUT
    @Path("projects/{projectKey}/statuses/{statusKey}")
    public Response updateStatus(@PathParam("projectKey") final String projectKey,
                                 @PathParam("statusKey") final String statusKey,
                                 final StatusDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.updateStatus(user(), projectKey, statusKey, draft)).build();
            }
        });
    }

    @DELETE
    @Path("projects/{projectKey}/statuses/{statusKey}")
    public Response deleteStatus(@PathParam("projectKey") final String projectKey,
                                 @PathParam("statusKey") final String statusKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteStatus(user(), projectKey, statusKey);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/grants")
    public Response grants(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.listGrants(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/grants")
    public Response addGrant(@PathParam("projectKey") final String projectKey, final GrantDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED)
                        .entity(assetService.addGrant(user(), projectKey, draft))
                        .build();
            }
        });
    }

    @DELETE
    @Path("projects/{projectKey}/grants")
    public Response deleteGrant(@PathParam("projectKey") final String projectKey,
                                @QueryParam("group") final String group,
                                @QueryParam("user") final String userKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                if (userKey != null && !userKey.trim().isEmpty()) {
                    assetService.deleteUserGrant(user(), projectKey, userKey);
                } else {
                    assetService.deleteGrant(user(), projectKey, group);
                }
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("groups")
    public Response groups(@QueryParam("q") final String query) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.searchGroups(user(), query)).build();
            }
        });
    }

    @GET
    @Path("asset-fields")
    public Response assetFields() {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                if (user() == null) {
                    throw new AssetException(401, "asset-tree.error.auth");
                }
                PortalFieldsDto dto = new PortalFieldsDto();
                dto.setFields(PortalLookup.assetFieldIds());
                return Response.ok(dto).build();
            }
        });
    }

    @GET
    @Path("portals/{portalId}")
    public Response portalProject(@PathParam("portalId") final int portalId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                if (user() == null) {
                    throw new AssetException(401, "asset-tree.error.auth");
                }
                String projectKey = PortalLookup.projectKeyForPortal(user(), portalId);
                if (projectKey == null || projectKey.isEmpty()) {
                    throw new AssetException(404, "asset-tree.error.project.notFound");
                }
                PortalProjectDto dto = new PortalProjectDto();
                dto.setProjectKey(projectKey);
                return Response.ok(dto).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/card")
    public Response cardLayout(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.cardLayout(user(), projectKey)).build();
            }
        });
    }

    @PUT
    @Path("projects/{projectKey}/card")
    public Response saveCardLayout(@PathParam("projectKey") final String projectKey, final CardLayout draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveCardLayout(user(), projectKey, draft)).build();
            }
        });
    }

    @PUT
    @Path("projects/{projectKey}/asset-key")
    public Response saveAssetKey(@PathParam("projectKey") final String projectKey, final AssetKeyDto draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveAssetKey(user(), projectKey, draft)).build();
            }
        });
    }

    @PUT
    @Path("projects/{projectKey}/view")
    public Response saveProjectView(@PathParam("projectKey") final String projectKey, final ProjectViewDto draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveProjectView(user(), projectKey, draft)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/picker")
    public Response picker(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.picker(user(), projectKey)).build();
            }
        });
    }

    @GET
    @Path("portal/assets/{id}")
    public Response portalAsset(@PathParam("id") final int id) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.portalAsset(user(), id)).build();
            }
        });
    }

    @GET
    @Path("portal/assets/{id}/requests")
    public Response portalRequestForm(@PathParam("id") final int id) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.portalRequestForm(user(), id)).build();
            }
        });
    }

    @POST
    @Path("portal/assets/{id}/intent")
    public Response rememberPortalAsset(@PathParam("id") final int id) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.rememberPortalAsset(user(), id);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("portal/assets/{id}/requests/{issueKey}")
    public Response portalLinkRequest(@PathParam("id") final int id, @PathParam("issueKey") final String issueKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.portalLinkRequest(user(), id, issueKey);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("portal/assets/{id}/requests")
    public Response createPortalRequest(@PathParam("id") final int id, final PortalRequestDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.createPortalRequest(user(), id, draft)).build();
            }
        });
    }

    @POST
    @Path("portal/assets/{id}/comments")
    public Response portalComment(@PathParam("id") final int id, final CommentDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                String body = draft == null ? null : draft.getBody();
                return Response.status(Response.Status.CREATED).entity(assetService.portalComment(user(), id, body, false)).build();
            }
        });
    }

    @DELETE
    @Path("portal/assets/{id}/files/{fileId}")
    public Response portalDeleteFile(@PathParam("id") final int id, @PathParam("fileId") final int fileId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.portalDeleteFile(user(), id, fileId);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("portal/assets/{id}/shares")
    public Response portalShare(@PathParam("id") final int id, final ShareDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                String userKey = draft == null ? null : draft.getUserKey();
                return Response.status(Response.Status.CREATED).entity(assetService.portalShare(user(), id, userKey)).build();
            }
        });
    }

    @DELETE
    @Path("portal/assets/{id}/shares/{userKey}")
    public Response portalUnshare(@PathParam("id") final int id, @PathParam("userKey") final String userKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.portalUnshare(user(), id, userKey);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("portal/mine")
    public Response portalMine() {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.portalMine(user())).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/portal-rules")
    public Response portalRules(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.listPortalRules(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/portal-rules")
    public Response addPortalRule(@PathParam("projectKey") final String projectKey, final PortalRuleDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED)
                        .entity(assetService.addPortalRule(user(), projectKey, draft))
                        .build();
            }
        });
    }

    @PUT
    @Path("projects/{projectKey}/portal-rules/{ruleId}")
    public Response updatePortalRule(@PathParam("projectKey") final String projectKey, @PathParam("ruleId") final int ruleId,
                                     final PortalRuleDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.updatePortalRule(user(), projectKey, ruleId, draft)).build();
            }
        });
    }

    @DELETE
    @Path("projects/{projectKey}/portal-rules/{ruleId}")
    public Response deletePortalRule(@PathParam("projectKey") final String projectKey, @PathParam("ruleId") final int ruleId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deletePortalRule(user(), projectKey, ruleId);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/issue-statuses")
    public Response issueStatuses(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.listIssueStatuses(user(), projectKey)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/issue-types")
    public Response issueTypes(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.listIssueTypes(user(), projectKey)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/flows")
    public Response flowRules(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.listFlowRules(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/flows")
    public Response saveFlowRule(@PathParam("projectKey") final String projectKey, final FlowRuleDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED)
                        .entity(assetService.saveFlowRule(user(), projectKey, draft))
                        .build();
            }
        });
    }

    @DELETE
    @Path("projects/{projectKey}/flows/{ruleId}")
    public Response deleteFlowRule(@PathParam("projectKey") final String projectKey, @PathParam("ruleId") final int ruleId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteFlowRule(user(), projectKey, ruleId);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/flow-graph")
    public Response flowGraph(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.flowGraph(user(), projectKey)).build();
            }
        });
    }

    @PUT
    @Path("projects/{projectKey}/flow-graph")
    public Response saveFlowGraph(@PathParam("projectKey") final String projectKey, final FlowBookDto draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveFlowGraph(user(), projectKey, draft)).build();
            }
        });
    }

    @GET
    @Path("users")
    public Response users(@QueryParam("q") final String query) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.searchUsers(user(), query)).build();
            }
        });
    }

    @GET
    @Path("users/{userKey}")
    public Response userProfile(@PathParam("userKey") final String userKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.userProfile(user(), userKey)).build();
            }
        });
    }

    @GET
    @Path("users/{userKey}/assets")
    public Response userAssets(@PathParam("userKey") final String userKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.assetsForUser(user(), userKey)).build();
            }
        });
    }

    @GET
    @Path("issues/{issueId}/context")
    public Response issueContext(@PathParam("issueId") final long issueId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.issueContext(user(), issueId)).build();
            }
        });
    }

    @GET
    @Path("issues/{issueId}/search")
    public Response searchForIssue(@PathParam("issueId") final long issueId, @QueryParam("q") final String query) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.searchForIssue(user(), issueId, query)).build();
            }
        });
    }

    @PUT
    @Path("types/{typeKey}")
    public Response updateType(@PathParam("typeKey") final String typeKey, final TypeDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.updateType(user(), typeKey, draft)).build();
            }
        });
    }

    @DELETE
    @Path("types/{typeKey}")
    public Response deleteType(@PathParam("typeKey") final String typeKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteType(user(), typeKey);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("tni/sight")
    public Response tniSight(final TniSightDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.rememberTni(user(), draft);
                return Response.noContent().build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/subnets")
    public Response tniSubnets(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.tniSubnets(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/subnets")
    public Response createTniSubnet(@PathParam("projectKey") final String projectKey, final TniSubnetDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.CREATED).entity(assetService.createTniSubnet(user(), projectKey, draft)).build();
            }
        });
    }

    @DELETE
    @Path("projects/{projectKey}/subnets/{subnetId}")
    public Response deleteTniSubnet(@PathParam("projectKey") final String projectKey, @PathParam("subnetId") final int subnetId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                assetService.deleteTniSubnet(user(), projectKey, subnetId);
                return Response.noContent().build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/subnets/{subnetId}/scan")
    public Response scanTniSubnet(@PathParam("projectKey") final String projectKey, @PathParam("subnetId") final int subnetId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.status(Response.Status.ACCEPTED).entity(assetService.scanTniSubnet(user(), projectKey, subnetId)).build();
            }
        });
    }

    @GET
    @Path("assets/{id}/watch")
    public Response tniWatch(@PathParam("id") final int assetId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.tniWatch(user(), assetId)).build();
            }
        });
    }

    @POST
    @Path("assets/{id}/ping")
    public Response pingTni(@PathParam("id") final int assetId) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.pingTni(user(), assetId)).build();
            }
        });
    }

    @PUT
    @Path("assets/{id}/watch")
    public Response saveTniWatch(@PathParam("id") final int assetId, final TniWatchDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveTniWatch(user(), assetId, draft)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/directory")
    public Response tniDirectory(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.tniDirectory(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/directory")
    public Response saveTniDirectory(@PathParam("projectKey") final String projectKey, final TniSourceDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveTniDirectory(user(), projectKey, draft)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/directory/load")
    public Response loadTniDirectory(@PathParam("projectKey") final String projectKey, final TniSourceDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.loadTniDirectory(user(), projectKey, draft)).build();
            }
        });
    }

    @GET
    @Path("projects/{projectKey}/sccm")
    public Response tniSccm(@PathParam("projectKey") final String projectKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.tniSccm(user(), projectKey)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/sccm")
    public Response saveTniSccm(@PathParam("projectKey") final String projectKey, final TniSourceDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.saveTniSccm(user(), projectKey, draft)).build();
            }
        });
    }

    @POST
    @Path("projects/{projectKey}/sccm/load")
    public Response loadTniSccm(@PathParam("projectKey") final String projectKey, final TniSourceDraft draft) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                return Response.ok(assetService.loadTniSccm(user(), projectKey, draft)).build();
            }
        });
    }

    private Response receiveJson(final String projectKey, final JsonImportDraft draft, final String idempotencyKey) {
        return invoke(new Callable<Response>() {
            @Override
            public Response call() {
                if (draft != null && (draft.getIdempotencyKey() == null || draft.getIdempotencyKey().trim().isEmpty())
                        && idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                    draft.setIdempotencyKey(idempotencyKey.trim());
                }
                return Response.ok(assetService.importJson(user(), projectKey, draft)).build();
            }
        });
    }

    private void applyIdempotency(ImportDraft draft, String header) {
        if (draft == null) {
            return;
        }
        if ((draft.getIdempotencyKey() == null || draft.getIdempotencyKey().trim().isEmpty())
                && header != null && !header.trim().isEmpty()) {
            draft.setIdempotencyKey(header.trim());
        }
    }

    private ApplicationUser user() {
        return authenticationContext().getLoggedInUser();
    }

    private JiraAuthenticationContext authenticationContext() {
        return ComponentAccessor.getJiraAuthenticationContext();
    }

    private Response invoke(Callable<Response> action) {
        try {
            return action.call();
        } catch (AssetException ex) {
            return message(ex.getStatus(), ex.getMessageKey(), ex.getArgs());
        } catch (Exception ex) {
            log.error("Asset tree request failed", ex);
            boolean reading = request != null && "GET".equalsIgnoreCase(request.getMethod());
            return message(500, reading ? "asset-tree.error.load" : "asset-tree.error.unexpected");
        }
    }

    private Response message(int status, String key, Object... args) {
        I18nHelper i18n = authenticationContext().getI18nHelper();
        String text = args == null || args.length == 0 ? i18n.getText(key) : i18n.getText(key, args);
        return Response.status(status).entity(new MessageDto(text)).build();
    }
}
