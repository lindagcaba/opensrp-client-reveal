package org.smartregister.reveal.fragment;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;

import org.json.JSONObject;
import org.smartregister.cursoradapter.RecyclerViewPaginatedAdapter;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.BaseDrawerContract;
import org.smartregister.reveal.contract.EventRegisterContract;
import org.smartregister.reveal.model.EventRegisterDetails;
import org.smartregister.reveal.model.FilterConfiguration;
import org.smartregister.reveal.model.TaskFilterParams;
import org.smartregister.reveal.presenter.EventRegisterFragmentPresenter;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.BusinessStatus;
import org.smartregister.reveal.util.Constants.JsonForm;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.reveal.view.DrawerMenuView;
import org.smartregister.reveal.view.EventRegisterActivity;
import org.smartregister.reveal.view.FilterTasksActivity;
import org.smartregister.reveal.view.ListTasksActivity;
import org.smartregister.reveal.viewholder.EventViewHolder;
import org.smartregister.view.fragment.BaseRegisterFragment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static org.smartregister.reveal.util.Constants.Filter.FILTER_CONFIGURATION;
import static org.smartregister.reveal.util.Constants.Filter.FILTER_SORT_PARAMS;
import static org.smartregister.reveal.util.Constants.RequestCode.REQUEST_CODE_FILTER_TASKS;

/**
 * Created by samuelgithengi on 7/30/20.
 */
public class EventRegisterFragment extends BaseRegisterFragment implements EventRegisterContract.View, BaseDrawerContract.DrawerActivity {

    private BaseDrawerContract.View drawerView;

    private ProgressDialog progressDialog;

    private RevealJsonFormUtils jsonFormUtils;

    private TextView filterTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        drawerView = new DrawerMenuView(this);
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);

    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_event_register;
    }

    @Override
    protected void initializePresenter() {
        presenter = new EventRegisterFragmentPresenter(this, Constants.EventsRegister.VIEW_IDENTIFIER);
    }

    @Override
    public void initializeAdapter(Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        EventViewHolder eventViewHolder = new EventViewHolder(getContext(), registerActionHandler, paginationViewHandler);
        clientAdapter = new RecyclerViewPaginatedAdapter(null, eventViewHolder, context().commonrepository(this.tablename));
        clientAdapter.setCurrentlimit(20);
        clientsView.setAdapter(clientAdapter);
    }

    @Override
    public void startMapActivity() {
        Intent intent = new Intent(getContext(), ListTasksActivity.class);
        getActivity().setResult(RESULT_OK, intent);
        getActivity().finish();
    }


    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        view.findViewById(R.id.txt_map_label).setOnClickListener(v -> getPresenter().onOpenMapClicked());

        drawerView.initializeDrawerLayout();
        view.findViewById(R.id.drawerMenu).setOnClickListener(v -> drawerView.openDrawerLayout());
        drawerView.onResume();

        getSearchView().setHint(R.string.search);

        filterTextView = view.findViewById(R.id.filter_text_view);
        filterTextView.setOnClickListener(v -> {
            getPresenter().onFilterTasksClicked();
        });

    }

    @Override
    public void setUniqueID(String s) {//not used
    }


    @Override
    public void setAdvancedSearchFormData(HashMap<String, String> hashMap) {//not used
    }

    @Override
    protected String getMainCondition() {
        return getPresenter().getMainCondition();
    }

    @Override
    protected String getDefaultSortQuery() {
        return Constants.DatabaseKeys.EVENT_DATE + " DESC";
    }

    @Override
    protected void startRegistration() {//not used
    }

    @Override
    protected void onViewClicked(View view) {
        EventRegisterDetails details = (EventRegisterDetails) view.getTag(R.id.patient_column);
        getPresenter().onEventSelected(details);
    }

    @Override
    public void showNotFoundPopup(String s) {//not used
    }

    private EventRegisterContract.Presenter getPresenter() {
        return (EventRegisterContract.Presenter) presenter;
    }

    @Override
    public void onDrawerClosed() {//Do nothing
    }

    @Override
    public RevealJsonFormUtils getJsonFormUtils() {
        return jsonFormUtils;
    }

    public void setJsonFormUtils(RevealJsonFormUtils jsonFormUtils) {
        this.jsonFormUtils = jsonFormUtils;
    }

    @Override
    public void startForm(JSONObject formName) {
        ((EventRegisterActivity) getActivity()).startFormActivity(formName);
    }

    @Override
    public void displayError(int title, int message) {
        new AlertDialog.Builder(getActivity()).setTitle(title).setMessage(message).create().show();
    }

    @Override
    public void showProgressDialog(@StringRes int title, @StringRes int message) {
        if (progressDialog != null) {
            progressDialog.setTitle(title);
            progressDialog.setMessage(getString(message));
            progressDialog.show();
        }
    }

    @Override
    public void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void openFilterActivity(TaskFilterParams filterParams) {
        Intent intent = new Intent(getContext(), FilterTasksActivity.class);
        intent.putExtra(FILTER_SORT_PARAMS, filterParams);
        intent.putExtra(FILTER_CONFIGURATION, FilterConfiguration.builder()
                .businessStatusLayoutEnabled(true)
                .businessStatusList(Arrays.asList(BusinessStatus.SPRAYED, BusinessStatus.NOT_SPRAYED, BusinessStatus.NOT_ELIGIBLE))
                .interventionTypeLayoutEnabled(false)
                .taskCodeLayoutEnabled(false)
                .formsLayoutEnabled(true)
                .formsList(Arrays.asList(JsonForm.SPRAY_FORM_ZAMBIA, JsonForm.VERIFICATION_FORM_ZAMBIA,
                        JsonForm.DAILY_SUMMARY_ZAMBIA, JsonForm.TEAM_LEADER_DOS_ZAMBIA,
                        JsonForm.MOBILIZATION_FORM_ZAMBIA, JsonForm.IRS_SA_DECISION_ZAMBIA,
                        JsonForm.IRS_FIELD_OFFICER_ZAMBIA))
                .build());
        getActivity().startActivityForResult(intent, REQUEST_CODE_FILTER_TASKS);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_FILTER_TASKS && resultCode == RESULT_OK && data.hasExtra(FILTER_SORT_PARAMS)) {
            TaskFilterParams filterParams = (TaskFilterParams) data.getSerializableExtra(FILTER_SORT_PARAMS);
            getPresenter().filterTasks(filterParams);
        }
    }


}