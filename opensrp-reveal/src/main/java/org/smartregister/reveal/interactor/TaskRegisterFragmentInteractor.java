package org.smartregister.reveal.interactor;

import android.location.Location;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.common.annotations.VisibleForTesting;

import net.sqlcipher.Cursor;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.domain.Task.TaskStatus;
import org.smartregister.family.util.DBConstants;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.LocationRepository;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.TaskRegisterFragmentContract;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.EventType;
import org.smartregister.reveal.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.smartregister.family.util.DBConstants.KEY.FIRST_NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.BASE_ENTITY_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.BUSINESS_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.CODE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.COMPLETED_TASK_COUNT;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FAMILY_NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FOR;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.LATITUDE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.LONGITUDE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.NOT_SRAYED_OTHER_REASON;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.NOT_SRAYED_REASON;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.OTHER;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.PLAN_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAYED_STRUCTURES;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAY_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURES_TABLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TASK_COUNT;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TASK_TABLE;
import static org.smartregister.reveal.util.Constants.Intervention.BCC;
import static org.smartregister.reveal.util.Constants.Intervention.CASE_CONFIRMATION;
import static org.smartregister.reveal.util.FamilyConstants.TABLE_NAME.FAMILY;
import static org.smartregister.reveal.util.FamilyConstants.TABLE_NAME.FAMILY_MEMBER;

/**
 * Created by samuelgithengi on 3/18/19.
 */
public class TaskRegisterFragmentInteractor extends BaseInteractor {

    private final LocationRepository locationRepository;
    private final Float locationBuffer;

    public TaskRegisterFragmentInteractor(TaskRegisterFragmentContract.Presenter presenter) {
        this(presenter, Utils.getLocationBuffer());
    }

    @VisibleForTesting
    public TaskRegisterFragmentInteractor(TaskRegisterFragmentContract.Presenter presenter,
                                          Float locationBuffer) {
        super(presenter);
        this.locationBuffer = locationBuffer;
        locationRepository = RevealApplication.getInstance().getLocationRepository();
    }

    private String mainSelect(String mainCondition) {
        String tableName = TASK_TABLE;
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName), ID);
        queryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s ",
                STRUCTURES_TABLE, tableName, FOR, STRUCTURES_TABLE, ID));
        queryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s ",
                SPRAYED_STRUCTURES, tableName, FOR, SPRAYED_STRUCTURES, DBConstants.KEY.BASE_ENTITY_ID));
        queryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s ",
                FAMILY, STRUCTURES_TABLE, ID, FAMILY, STRUCTURE_ID));
        return queryBuilder.mainCondition(mainCondition);
    }

    private String nonRegisteredStructureTasksSelect(String mainCondition) {
        String tableName = TASK_TABLE;
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName), ID);
        queryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s ",
                STRUCTURES_TABLE, tableName, FOR, STRUCTURES_TABLE, ID));
        queryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s ",
                SPRAYED_STRUCTURES, tableName, FOR, SPRAYED_STRUCTURES, DBConstants.KEY.BASE_ENTITY_ID));
        queryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s ",
                FAMILY, STRUCTURES_TABLE, ID, FAMILY, STRUCTURE_ID));
        queryBuilder.mainCondition(mainCondition);
        return queryBuilder.addCondition(String.format(" AND %s.%s IS NULL",
                FAMILY, STRUCTURE_ID));
    }

    private String groupedRegisteredStructureTasksSelect(String mainCondition) {
        String tableName = TASK_TABLE;
        SmartRegisterQueryBuilder structureTasksQueryBuilder = new SmartRegisterQueryBuilder();
        structureTasksQueryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName), ID);
        structureTasksQueryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s ",
                STRUCTURES_TABLE, tableName, FOR, STRUCTURES_TABLE, ID));
        structureTasksQueryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s  COLLATE NOCASE",
                FAMILY, STRUCTURES_TABLE, ID, FAMILY, STRUCTURE_ID));
        structureTasksQueryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s ",
                SPRAYED_STRUCTURES, tableName, FOR, SPRAYED_STRUCTURES, DBConstants.KEY.BASE_ENTITY_ID));
        structureTasksQueryBuilder.mainCondition(mainCondition);

        SmartRegisterQueryBuilder familyMemberTasksQueryBuilder = new SmartRegisterQueryBuilder();
        familyMemberTasksQueryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName), ID);
        familyMemberTasksQueryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s ",
                FAMILY_MEMBER, TASK_TABLE, FOR, FAMILY_MEMBER, BASE_ENTITY_ID));
        familyMemberTasksQueryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s ",
                STRUCTURES_TABLE, STRUCTURES_TABLE, ID, FAMILY_MEMBER, STRUCTURE_ID));
        familyMemberTasksQueryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s  COLLATE NOCASE",
                FAMILY, STRUCTURES_TABLE, ID, FAMILY, STRUCTURE_ID));
        familyMemberTasksQueryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s ",
                SPRAYED_STRUCTURES, tableName, FOR, SPRAYED_STRUCTURES, DBConstants.KEY.BASE_ENTITY_ID));
        familyMemberTasksQueryBuilder.mainCondition(mainCondition);

        return String.format(" SELECT %s.*, SUM(CASE WHEN %s.status='COMPLETED' THEN 1 ELSE 0 END) AS %s , COUNT(%s._id) AS %s FROM ( ",
                "tasks", "tasks", COMPLETED_TASK_COUNT, "tasks", TASK_COUNT) + structureTasksQueryBuilder +
                " UNION " + familyMemberTasksQueryBuilder + " ) AS tasks GROUP BY tasks.structure_id ";

    }

    private String bccSelect() {
        return String.format("SELECT * FROM %s WHERE %s = ? AND %s = ? AND %s != ? AND %s ='%s'",
                TASK_TABLE, FOR, PLAN_ID, STATUS, CODE, BCC);
    }

    private String indexCaseSelect() {
        return String.format("SELECT * FROM %s WHERE %s = ? AND %s != ? AND %s = ?",
                TASK_TABLE, PLAN_ID, STATUS, CODE);
    }


    private String[] mainColumns(String tableName) {
        return new String[]{
                tableName + "." + ID,
                tableName + "." + CODE,
                tableName + "." + FOR,
                tableName + "." + BUSINESS_STATUS,
                tableName + "." + STATUS,
                STRUCTURES_TABLE + "." + LATITUDE,
                STRUCTURES_TABLE + "." + LONGITUDE,
                STRUCTURES_TABLE + "." + NAME,
                SPRAYED_STRUCTURES + "." + STRUCTURE_NAME,
                SPRAYED_STRUCTURES + "." + FAMILY_NAME,
                SPRAYED_STRUCTURES + "." + SPRAY_STATUS,
                SPRAYED_STRUCTURES + "." + NOT_SRAYED_REASON,
                SPRAYED_STRUCTURES + "." + NOT_SRAYED_OTHER_REASON,
                STRUCTURES_TABLE + "." + ID + " AS " + STRUCTURE_ID,
                FAMILY + "." + FIRST_NAME

        };
    }


    public void findTasks(Pair<String, String[]> mainCondition, Location lastLocation, Location operationalAreaCenter, String houseLabel) {
        if (mainCondition == null || mainCondition.second == null || mainCondition.second.length != 3 || mainCondition.second[0] == null) {
            getPresenter().onTasksFound(null, 0);
            return;
        }
        // Fetch grouped tasks
        List<TaskDetails> tasks = new ArrayList<>();
        appExecutors.diskIO().execute(() -> {
            int structuresWithinBuffer = 0;
            String groupId = mainCondition.second[0];
            String planId = mainCondition.second[1];
            if (Utils.getInterventionLabel() == R.string.focus_investigation) { // perform task grouping

                tasks.addAll(queryTaskDetails(groupedRegisteredStructureTasksSelect(mainCondition.first),
                        new String[]{groupId, planId, groupId, planId}, lastLocation, operationalAreaCenter, houseLabel, structuresWithinBuffer, true));


                tasks.addAll(queryTaskDetails(nonRegisteredStructureTasksSelect(mainCondition.first),
                        mainCondition.second, lastLocation, operationalAreaCenter, houseLabel, structuresWithinBuffer, false));

            } else {

                tasks.addAll(queryTaskDetails(mainSelect(mainCondition.first), mainCondition.second,
                        lastLocation, operationalAreaCenter, houseLabel, structuresWithinBuffer, false));

            }

            // Query BCC task
            tasks.addAll(queryTaskDetails(bccSelect(), mainCondition.second, lastLocation,
                    operationalAreaCenter, houseLabel, structuresWithinBuffer, false));


            // Query Case Confirmation task
            tasks.addAll(queryTaskDetails(indexCaseSelect(), new String[]{planId, TaskStatus.CANCELLED.name(),
                            CASE_CONFIRMATION}, lastLocation,
                    operationalAreaCenter, houseLabel, structuresWithinBuffer, false));

            Collections.sort(tasks);
            appExecutors.mainThread().execute(() -> {
                getPresenter().onTasksFound(tasks, structuresWithinBuffer);
            });

        });

    }

    private List<TaskDetails> queryTaskDetails(String query, String[] params, Location lastLocation,
                                               Location operationalAreaCenter, String houseLabel, Integer structuresWithinBuffer, boolean groupedTasks) {
        List<TaskDetails> tasks = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, params);

            while (cursor.moveToNext()) {
                TaskDetails taskDetails = readTaskDetails(cursor, lastLocation, operationalAreaCenter, houseLabel, groupedTasks);
                if (taskDetails.getDistanceFromUser() <= locationBuffer) {
                    structuresWithinBuffer += 1;
                }
                tasks.add(taskDetails);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return tasks;
    }


    private TaskDetails readTaskDetails(Cursor cursor, Location lastLocation, Location operationalAreaCenter, String houseLabel, boolean isGroupedTasks) {
        TaskDetails task = new TaskDetails(cursor.getString(cursor.getColumnIndex(ID)));
        task.setTaskCode(cursor.getString(cursor.getColumnIndex(CODE)));
        task.setTaskEntity(cursor.getString(cursor.getColumnIndex(FOR)));
        task.setBusinessStatus(cursor.getString(cursor.getColumnIndex(BUSINESS_STATUS)));
        task.setTaskStatus(cursor.getString(cursor.getColumnIndex(STATUS)));
        if (isGroupedTasks) {
            task.setTaskCount(cursor.getInt(cursor.getColumnIndex(TASK_COUNT)));
            task.setCompleteTaskCount(cursor.getInt(cursor.getColumnIndex(COMPLETED_TASK_COUNT)));
        }
        Location location = new Location((String) null);

        if (!BCC.equals(task.getTaskCode()) && !CASE_CONFIRMATION.equals(task.getTaskCode())) {
            location.setLatitude(cursor.getDouble(cursor.getColumnIndex(LATITUDE)));
            location.setLongitude(cursor.getDouble(cursor.getColumnIndex(LONGITUDE)));
            task.setLocation(location);
            task.setStructureName(cursor.getString(cursor.getColumnIndex(NAME)));
            if (StringUtils.isBlank(task.getStructureName())) {
                task.setStructureName(cursor.getString(cursor.getColumnIndex(STRUCTURE_NAME)));
            }

            task.setFamilyName(cursor.getString(cursor.getColumnIndex(FIRST_NAME)));
            if (task.getFamilyName() == null) {
                task.setFamilyName(cursor.getString(cursor.getColumnIndex(FAMILY_NAME)));
            }

            if (task.getFamilyName() != null)
                task.setFamilyName(task.getFamilyName() + " " + houseLabel);

            task.setSprayStatus(cursor.getString(cursor.getColumnIndex(SPRAY_STATUS)));

            if (Constants.BusinessStatus.NOT_SPRAYED.equals(task.getBusinessStatus())) {
                String reason = cursor.getString(cursor.getColumnIndex(NOT_SRAYED_REASON));
                if (OTHER.equals(reason)) {
                    reason = cursor.getString(cursor.getColumnIndex(NOT_SRAYED_OTHER_REASON));
                }
                task.setTaskDetails(reason);
            }
            task.setStructureId(cursor.getString(cursor.getColumnIndex(STRUCTURE_ID)));
        }
        if (BCC.equals(task.getTaskCode())) {
            //set distance to -2 to always display on top of register
            task.setDistanceFromUser(-2);
        } else if (CASE_CONFIRMATION.equals(task.getTaskCode())) {
            //set distance to -1 to always display on top of register and below BCC
            task.setDistanceFromUser(-1);
        } else if (lastLocation != null) {
            task.setDistanceFromUser(location.distanceTo(lastLocation));
        } else {
            task.setDistanceFromUser(location.distanceTo(operationalAreaCenter));
            task.setDistanceFromCenter(true);
        }
        return task;
    }


    public void calculateDistanceFromUser(List<TaskDetails> tasks, Location location) {
        if (tasks == null)
            return;
        appExecutors.diskIO().execute(() -> {
            int structuresWithinBuffer = 0;
            for (TaskDetails taskDetails : tasks) {
                if (!BCC.equals(taskDetails.getTaskCode())) {
                    taskDetails.setDistanceFromUser(taskDetails.getLocation().distanceTo(location));
                    taskDetails.setDistanceFromCenter(false);
                }
                if (taskDetails.getDistanceFromUser() <= locationBuffer) {
                    structuresWithinBuffer += 1;
                }
            }
            Collections.sort(tasks);
            int finalStructuresWithinBuffer = structuresWithinBuffer;
            appExecutors.mainThread().execute(() -> {
                getPresenter().onTasksFound(tasks, finalStructuresWithinBuffer);
            });
        });

    }


    public void getStructure(TaskDetails taskDetails) {
        appExecutors.diskIO().execute(() -> {
            org.smartregister.domain.Location structure;
            if (BCC.equals(taskDetails.getTaskCode()))
                structure = locationRepository.getLocationById(taskDetails.getTaskEntity());
            else
                structure = structureRepository.getLocationById(taskDetails.getStructureId());
            appExecutors.mainThread().execute(() -> {
                getPresenter().onStructureFound(structure, taskDetails);
            });
        });
    }

    private TaskRegisterFragmentContract.Presenter getPresenter() {
        return (TaskRegisterFragmentContract.Presenter) presenterCallBack;
    }

    public void getIndexCaseDetails(String structureId, String operationalArea, String currentPlanId) {
        appExecutors.diskIO().execute(() -> {
            if (StringUtils.isBlank(structureId) && StringUtils.isBlank(operationalArea)) {
                return;
            }
            JSONObject jsonEvent = new JSONObject();
            Cursor cursor = null;
            try {
                String id = structureId != null ? structureId : operationalArea;
                cursor = getDatabase().rawQuery("SELECT json FROM "
                                + EventClientRepository.Table.event.name()
                                + " WHERE "
                                + EventClientRepository.event_column.baseEntityId.name()
                                + " IN( ?) AND " + EventClientRepository.event_column.eventType.name() + "= ? ",
                        new String[]{id, EventType.CASE_DETAILS_EVENT});
                if (cursor.moveToNext()) {
                    String jsonEventStr = cursor.getString(0);

                    jsonEventStr = jsonEventStr.replaceAll("'", "");

                    jsonEvent = new JSONObject(jsonEventStr);
                }
            } catch (Exception e) {
                Log.e(getClass().getName(), "Exception", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            JSONObject finalJsonEvent = jsonEvent;
            appExecutors.mainThread().execute(() -> {
                getPresenter().onIndexCaseFound(finalJsonEvent);
            });
        });

    }
}
