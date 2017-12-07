package org.smartregister.bidan.fragment;

import android.content.Intent;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.opensrp.api.domain.Location;
import org.opensrp.api.util.EntityUtils;
import org.opensrp.api.util.LocationTree;
import org.opensrp.api.util.TreeNode;
import org.smartregister.Context;
import org.smartregister.bidan.R;
import org.smartregister.bidan.activity.DetailChildActivity;
import org.smartregister.bidan.activity.NativeKIAnakSmartRegisterActivity;
import org.smartregister.bidan.options.AnakOverviewServiceMode;
import org.smartregister.bidan.options.ChildFilterOption;
import org.smartregister.bidan.provider.ChildClientsProvider;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonPersonObjectController;
import org.smartregister.cursoradapter.CursorCommonObjectFilterOption;
import org.smartregister.cursoradapter.CursorCommonObjectSort;
import org.smartregister.cursoradapter.CursorSortOption;
import org.smartregister.cursoradapter.SmartRegisterPaginatedCursorAdapter;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.provider.SmartRegisterClientsProvider;
import org.smartregister.util.StringUtil;
import org.smartregister.view.activity.SecuredNativeSmartRegisterActivity;
import org.smartregister.view.contract.SmartRegisterClient;
import org.smartregister.view.controller.VillageController;
import org.smartregister.view.dialog.AllClientsFilter;
import org.smartregister.view.dialog.DialogOption;
import org.smartregister.view.dialog.DialogOptionMapper;
import org.smartregister.view.dialog.DialogOptionModel;
import org.smartregister.view.dialog.EditOption;
import org.smartregister.view.dialog.FilterOption;
import org.smartregister.view.dialog.ServiceModeOption;
import org.smartregister.view.dialog.SortOption;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.view.View.INVISIBLE;

//import com.flurry.android.FlurryAgent;
//import org.smartregister.bidan.lib.FlurryFacade;

/**
 * Created by koros on 10/29/15.
 */
public class AnakSmartRegisterFragment extends BaseSmartRegisterFragment
//        SecuredNativeSmartRegisterCursorAdapterFragment
{

    private static final String TAG = AnakSmartRegisterFragment.class.getSimpleName();
    //    WD
    public static String criteria;
    private final ClientActionHandler clientActionHandler = new ClientActionHandler();
    Date date = new Date();
    SimpleDateFormat sdf;
    Map<String, String> FS = new HashMap<>();
    private SmartRegisterClientsProvider clientProvider = null;
    private CommonPersonObjectController controller;
    private VillageController villageController;
    private DialogOptionMapper dialogOptionMapper;
    private String locationDialogTAG = "locationDialogTAG";

    public static String getCriteria() {
        return criteria;
    }

    public void setCriteria(String criteria) {
        this.criteria = criteria;
    }

    @Override
    protected void onCreation() {
    }

    @Override
    protected SecuredNativeSmartRegisterActivity.DefaultOptionsProvider getDefaultOptionsProvider() {

        return new SecuredNativeSmartRegisterActivity.DefaultOptionsProvider() {

            @Override
            public ServiceModeOption serviceMode() {
                return new AnakOverviewServiceMode(clientsProvider());
            }

            @Override
            public FilterOption villageFilter() {
                return new AllClientsFilter();
            }

            @Override
            public SortOption sortOption() {
                return
                        new CursorCommonObjectSort(getResources().getString(R.string.sort_by_name_label), AnakNameShort());

            }

            @Override
            public String nameInShortFormForTitle() {
                return Context.getInstance().getStringResource(R.string.child_register_title_in_short);
            }
        };
    }

    @Override
    protected SecuredNativeSmartRegisterActivity.NavBarOptionsProvider getNavBarOptionsProvider() {
        return new SecuredNativeSmartRegisterActivity.NavBarOptionsProvider() {

            @Override
            public DialogOption[] filterOptions() {
//                FlurryFacade.logEvent("click_filter_option_on_kohort_anak_dashboard");
                ArrayList<DialogOption> dialogOptionslist = new ArrayList<>();

                dialogOptionslist.add(new CursorCommonObjectFilterOption(getString(R.string.filter_by_all_label), filterStringForAll()));

                String locationJSON = context().anmLocationController().get();
                LocationTree locationTree = EntityUtils.fromJson(locationJSON, LocationTree.class);

                Map<String, TreeNode<String, Location>> locationMap =
                        locationTree.getLocationsHierarchy();
                addChildToList(dialogOptionslist, locationMap);
                DialogOption[] dialogOptions = new DialogOption[dialogOptionslist.size()];
                for (int i = 0; i < dialogOptionslist.size(); i++) {
                    dialogOptions[i] = dialogOptionslist.get(i);
                }

                return dialogOptions;
            }

            @Override
            public DialogOption[] serviceModeOptions() {
                return new DialogOption[]{};
            }

            @Override
            public DialogOption[] sortingOptions() {
//                FlurryFacade.logEvent("click_sorting_option_on_kohort_anak_dashboard");
                return new DialogOption[]{
                        new CursorCommonObjectSort(getResources().getString(R.string.sort_by_name_label), AnakNameShort()),
                        new CursorCommonObjectSort(getResources().getString(R.string.sort_by_name_label_reverse), AnakNameShortR()),
                        new CursorCommonObjectSort(getResources().getString(R.string.sort_by_dob_label), AnakDOB()),//tanggalLahirAnak
                };
            }

            @Override
            public String searchHint() {
                return getResources().getString(R.string.hh_search_hint);
            }
        };
    }

    private String AnakDOB() {
        return "tanggalLahirAnak ASC";
    }

    @Override
    protected SmartRegisterClientsProvider clientsProvider() {
//        if (clientProvider == null) {
//            clientProvider = new HouseHoldSmartClientsProvider(
//                    getActivity(),clientActionHandler , context.alertService());
//        }
        return null;
    }

    private DialogOption[] getEditOptions() {
        return ((NativeKIAnakSmartRegisterActivity) getActivity()).getEditOptions();
    }

    @Override
    protected void onInitialization() {
        //  context.formSubmissionRouter().getHandlerMap().put("census_enrollment_form", new CensusEnrollmentHandler());
    }

    @Override
    public void setupViews(View view) {
        getDefaultOptionsProvider();

        super.setupViews(view);
        view.findViewById(R.id.btn_report_month).setVisibility(INVISIBLE);
        view.findViewById(R.id.register_client).setVisibility(View.GONE);
        view.findViewById(R.id.service_mode_selection).setVisibility(View.GONE);
        clientsView.setVisibility(View.VISIBLE);
        clientsProgressView.setVisibility(View.INVISIBLE);
        initializeQueries(getCriteria());
    }

    private String filterStringForAll() {
        return "";
    }

    public void initializeQueries(String s){
        String tableName = "ec_anak";
        ChildClientsProvider childClientsProvider = new ChildClientsProvider(getActivity(),
                clientActionHandler, context().alertService(), context().commonrepository(tableName));
        clientAdapter = new SmartRegisterPaginatedCursorAdapter(getActivity(), null, childClientsProvider, context().commonrepository(tableName));
        clientsView.setAdapter(clientAdapter);

        setTablename(tableName);
        SmartRegisterQueryBuilder countqueryBuilder = new SmartRegisterQueryBuilder();
        countqueryBuilder.SelectInitiateMainTableCounts(tableName);
        countqueryBuilder.customJoin("LEFT JOIN ec_ibu ON ec_ibu.id = ec_anak.relational_id");

        mainCondition = "is_closed=0";
        countSelect = countqueryBuilder.mainCondition(mainCondition);
        super.CountExecute();


        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.SelectInitiateMainTableCounts(tableName);
        queryBuilder.SelectInitiateMainTable(tableName, new String[]{
                tableName + ".relationalid",
                tableName + ".details",
                tableName + ".is_closed",
                tableName + ".relational_id",
                tableName + ".tanggalLahirAnak",
                tableName + ".namaBayi",
        });

        queryBuilder.customJoin("LEFT JOIN ec_ibu ON ec_ibu.id = ec_anak.relational_id");

//        mainSelect = queryBuilder.mainCondition("ec_anak.is_closed = 0 and relationalid != ''");
        mainSelect = queryBuilder.mainCondition(mainCondition);

//        Sortqueries = AnakNameShort();
        Sortqueries = ((CursorSortOption) getDefaultOptionsProvider().sortOption()).sort();

        currentlimit = 20;
        currentoffset = 0;

        super.filterandSortInInitializeQueries();

        updateSearchView();
        refresh();
    }

    @Override
    public void startRegistration() {
    }

    private String AnakNameShort() {
        return "namaBayi ASC";
    }

    private String AnakNameShortR() {
        return "namaBayi DESC";
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        getDefaultOptionsProvider();
        if (isPausedOrRefreshList()) {
            initializeQueries("!");
        }
        //     updateSearchView();

    }

    private void updateSearchView() {
        getSearchView().removeTextChangedListener(textWatcher);
        getSearchView().addTextChangedListener(textWatcher);
    }

    public void addChildToList(ArrayList<DialogOption> dialogOptionslist, Map<String, TreeNode<String, Location>> locationMap) {
        for (Map.Entry<String, TreeNode<String, Location>> entry : locationMap.entrySet()) {

            if (entry.getValue().getChildren() != null) {
                addChildToList(dialogOptionslist, entry.getValue().getChildren());

            } else {
                StringUtil.humanize(entry.getValue().getLabel());
                String name = StringUtil.humanize(entry.getValue().getLabel());
                dialogOptionslist.add(new ChildFilterOption(name, "location_name", name, "ec_ibu"));

            }
        }
    }

    //    WD
    @Override
    public void setupSearchView(final View view) {
        searchView = (EditText) view.findViewById(org.smartregister.R.id.edt_search);
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (SmartShutterActivity.isDevCompat) {
//                    CharSequence selections[] = new CharSequence[]{"Name", "Photo"};
//                    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//                    builder.setTitle("Please Choose one, Search by");
//                    builder.setItems(selections, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int opt) {
//                            if (opt == 0) searchTextChangeListener("");
//                            else getFacialRecord(view);
//                        }
//                    });
//                    builder.show();
//                } else  {
                    searchTextChangeListener("");
//                }
            }
        });

        searchCancelView = view.findViewById(org.smartregister.R.id.btn_search_cancel);
        searchCancelView.setOnClickListener(searchCancelHandler);
    }

    public void getFacialRecord(View view) {
//        FlurryAgent.logEvent(TAG+" search_by_face", true);
        Log.e(TAG, "getFacialRecord: ");
        sdf = new SimpleDateFormat("hh:mm:ss.SS", Locale.ENGLISH);
        String face_start = sdf.format(date);
        FS.put("face_start", face_start);

//        SmartShutterActivity.kidetail = (CommonPersonObjectClient) view.getTag();
//        FlurryAgent.logEvent(TAG + " search_by_face", FS, true);
//
//        Intent intent = new Intent(getActivity(), SmartShutterActivity.class);
//        intent.putExtra("org.sid.sidface.ImageConfirmation.origin", TAG);
//        intent.putExtra("org.sid.sidface.ImageConfirmation.identify", true);
//        intent.putExtra("org.sid.sidface.ImageConfirmation.kidetail", (Parcelable) SmartShutterActivity.kidetail);
//        startActivityForResult(intent, 2);
    }

    public void searchTextChangeListener(String s) {
        Log.e(TAG, "searchTextChangeListener: " + s);
        if (s != null) {
            filters = s;
        } else {
            searchView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                }

                @Override
                public void onTextChanged(final CharSequence cs, int start, int before, int count) {

                    Log.e(TAG, "onTextChanged: searchTextChangeListener" + searchView.getText());
                    (new AsyncTask() {
//                    SmartRegisterClients filteredClients;

                        @Override
                        protected Object doInBackground(Object[] params) {
//                        currentSearchFilter =
//                        setCurrentSearchFilter(new HHSearchOption(cs.toString()));
//                        filteredClients = getClientsAdapter().getListItemProvider()
//                                .updateClients(getCurrentVillageFilter(), getCurrentServiceModeOption(),
//                                        getCurrentSearchFilter(), getCurrentSortOption());
//
                            filters = cs.toString();
                            joinTable = "";
                            mainCondition = "isClosed !='true' and ibuCaseId !='' ";
                            return null;
                        }
//
//                    @Override
//                    protected void onPostExecute(Object o) {
////                        clientsAdapter
////                                .refreshList(currentVillageFilter, currentServiceModeOption,
////                                        currentSearchFilter, currentSortOption);
////                        getClientsAdapter().refreshClients(filteredClients);
////                        getClientsAdapter().notifyDataSetChanged();
//                        getSearchCancelView().setVisibility(isEmpty(cs) ? INVISIBLE : VISIBLE);
//                        CountExecute();
//                        filterandSortExecute();
//                        super.onPostExecute(o);
//                    }
                    }).execute();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        Intent myIntent = new Intent(getActivity(), NativeKIAnakSmartRegisterActivity.class);
        if (data != null) {
            myIntent.putExtra("org.smartregister.bidan.face.face_mode", true);
            myIntent.putExtra("org.smartregister.bidan.face.base_id", data.getStringExtra("org.smartregister.bidan.face.base_id"));
        }
        getActivity().startActivity(myIntent);

    }

    private class ClientActionHandler implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.profile_info_layout:
//                    FlurryFacade.logEvent("click_detail_view_on_kohort_anak_dashboard");
                    DetailChildActivity.childclient = (CommonPersonObjectClient) view.getTag();
                    Intent intent = new Intent(getActivity(), DetailChildActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                    break;
                case R.id.btn_edit:
//                    FlurryFacade.logEvent("click_visit_button_on_kohort_anak_dashboard");
//                    showFragmentDialog(new EditDialogOptionModel(), view.getTag());
                    showFragmentDialog(((NativeKIAnakSmartRegisterActivity) getActivity()).new EditDialogOptionModel(), view.getTag());

                    break;
            }
        }
    }

    private class EditDialogOptionModelOld implements DialogOptionModel {
        @Override
        public DialogOption[] getDialogOptions() {
            return getEditOptions();
        }

        @Override
        public void onDialogOptionSelection(DialogOption option, Object tag) {
            onEditSelection((EditOption) option, (SmartRegisterClient) tag);
        }
    }

}