package org.smartregister.reveal.presenter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONObject;
import org.smartregister.reveal.contract.CallableInteractor;
import org.smartregister.reveal.contract.CallableInteractorCallBack;
import org.smartregister.reveal.contract.ChildProfileContract;
import org.smartregister.reveal.model.Child;
import org.smartregister.reveal.model.ChildProfileModel;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import timber.log.Timber;

public class ChildProfilePresenter implements ChildProfileContract.Presenter {

    private CallableInteractor callableInteractor;
    private WeakReference<ChildProfileContract.View> viewWeakReference;
    private ChildProfileContract.Model model;

    public ChildProfilePresenter(ChildProfileContract.View view) {
        this.viewWeakReference = new WeakReference<>(view);
    }

    @Override
    public void fetchProfileData(String baseEntityID) {
        CallableInteractor myInteractor = getInteractor();
        ChildProfileContract.Model myModel = getModel();

        if (getView() != null) {
            Callable<Child> callable = () -> myModel.getChild(baseEntityID);
            getView().setLoadingState(true);

            myInteractor.execute(callable, new CallableInteractorCallBack<Child>() {
                @Override
                public void onResult(Child result) {
                    ChildProfileContract.View view = getView();
                    if (view != null) {
                        if (result != null) {
                            view.onFetchResult(result);
                        } else {
                            view.onError(new IllegalStateException("Child not found"));
                        }
                        view.setLoadingState(false);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    ChildProfileContract.View view = getView();
                    if (view != null) {
                        view.onError(ex);
                        view.setLoadingState(false);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public CallableInteractor getInteractor() {
        if (callableInteractor == null)
            throw new IllegalStateException("Intercator is not set");

        return callableInteractor;
    }

    @Override
    public ChildProfileContract.Presenter usingInteractor(CallableInteractor interactor) {
        this.callableInteractor = interactor;
        return this;
    }

    @Nullable
    @Override
    public ChildProfileContract.View getView() {
        if (viewWeakReference != null)
            return viewWeakReference.get();

        return null;
    }

    @NonNull
    @Override
    public ChildProfileContract.Model getModel() {
        if (model == null)
            model = new ChildProfileModel();

        return model;
    }

    @Override
    public void startChildRegistrationForm(Context context, String baseEntityID) {
        CallableInteractor myInteractor = getInteractor();
        ChildProfileContract.Model model = getModel();
        Callable<JSONObject> callable = () -> model.getRegistrationEditForm(context, baseEntityID);
        myInteractor.execute(callable, new CallableInteractorCallBack<JSONObject>() {
            @Override
            public void onResult(JSONObject jsonObject) {
                ChildProfileContract.View view = getView();
                if (view != null) {
                    if (jsonObject != null) {
                        view.startJsonForm(jsonObject);
                    } else {
                        view.onError(new IllegalArgumentException("Form not found"));
                    }
                    view.setLoadingState(false);
                }
            }

            @Override
            public void onError(Exception ex) {
                ChildProfileContract.View view = getView();
                if (view != null) {
                    view.onError(ex);
                    view.setLoadingState(false);
                }
            }
        });
    }


    @Override
    public void onDestroy(boolean b) {
        Timber.v("onDestroy");
    }
}
