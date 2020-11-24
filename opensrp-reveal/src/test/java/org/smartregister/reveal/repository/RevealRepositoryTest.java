package org.smartregister.reveal.repository;

import com.evernote.android.job.JobManager;

import net.sqlcipher.database.SQLiteDatabase;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.DrishtiRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.shadow.SQLiteDatabaseShadow;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.FamilyConstants;

import java.util.ArrayList;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAYED_STRUCTURES;

/**
 * Created by samuelgithengi on 11/24/20.
 */
@Config(shadows = {SQLiteDatabaseShadow.class})
public class RevealRepositoryTest extends BaseUnitTest {

    @Mock
    private Context opensrpContext;

    private android.content.Context context = RuntimeEnvironment.application;

    @Mock
    private SQLiteDatabase sqLiteDatabase;

    @Mock
    private EventClientRepository eventClientRepository;

    @Mock
    private RevealClientProcessor revealClientProcessor;

    @Mock
    private AllSharedPreferences allSharedPreferences;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    private RevealRepository revealRepository;

    private JobManager jobManager;

    @Before
    public void setUp() {
        Context.bindtypes = new ArrayList<>();
        when(opensrpContext.sharedRepositoriesArray()).thenReturn(new DrishtiRepository[0]);
        revealRepository = new RevealRepository(context, opensrpContext);
        jobManager = spy(JobManager.create(context));
        Whitebox.setInternalState(JobManager.class, "instance", jobManager);
        Whitebox.setInternalState(RevealClientProcessor.class, "instance", revealClientProcessor);
        Whitebox.setInternalState(RevealApplication.getInstance(), "context", opensrpContext);
        when(opensrpContext.getEventClientRepository()).thenReturn(eventClientRepository);
        when(opensrpContext.allSharedPreferences()).thenReturn(allSharedPreferences);
    }

    @Test
    public void testOnCreateShouldCreateTablesAndIndexes() {
        int version = BuildConfig.DATABASE_VERSION;
        ReflectionHelpers.setStaticField(BuildConfig.class, "DATABASE_VERSION", 1);
        revealRepository.onCreate(sqLiteDatabase);
        verify(sqLiteDatabase, Mockito.times(35)).execSQL(stringArgumentCaptor.capture());
        for (String sql : stringArgumentCaptor.getAllValues()) {
            Assert.assertThat(sql, CoreMatchers.anyOf(CoreMatchers.containsStringIgnoringCase("CREATE TABLE"),
                    CoreMatchers.containsStringIgnoringCase("CREATE VIRTUAL TABLE"),
                    CoreMatchers.containsStringIgnoringCase("CREATE INDEX")));
        }
        ReflectionHelpers.setStaticField(BuildConfig.class, "DATABASE_VERSION", version);
    }

    @Test
    public void testOnCreateShouldCreateTableAndRunMigration2() {
        int version = BuildConfig.DATABASE_VERSION;
        ReflectionHelpers.setStaticField(BuildConfig.class, "DATABASE_VERSION", 2);
        revealRepository.onCreate(sqLiteDatabase);
        verify(sqLiteDatabase, Mockito.times(45)).execSQL(stringArgumentCaptor.capture());
        for (String sql : stringArgumentCaptor.getAllValues()) {
            Assert.assertThat(sql, CoreMatchers.anyOf(CoreMatchers.containsStringIgnoringCase("CREATE TABLE"),
                    CoreMatchers.containsStringIgnoringCase("CREATE VIRTUAL TABLE"),
                    CoreMatchers.containsStringIgnoringCase("CREATE INDEX"),
                    CoreMatchers.containsStringIgnoringCase("ALTER TABLE")));
        }
        ReflectionHelpers.setStaticField(BuildConfig.class, "DATABASE_VERSION", version);
        verify(jobManager).schedule(any());
        verify(eventClientRepository, timeout(6000)).fetchEventClientsByEventTypes(
                Arrays.asList(FamilyConstants.EventType.FAMILY_REGISTRATION, FamilyConstants.EventType.FAMILY_MEMBER_REGISTRATION,
                        FamilyConstants.EventType.UPDATE_FAMILY_REGISTRATION, FamilyConstants.EventType.UPDATE_FAMILY_MEMBER_REGISTRATION));
        verify(revealClientProcessor, timeout(5000)).processClient(any());
    }


    @Test
    public void testOnCreateShouldCreateTableRunsAllMigrations() throws Exception {
        revealRepository = spy(revealRepository);
        revealRepository.onCreate(sqLiteDatabase);
        verify(sqLiteDatabase, Mockito.atLeast(45)).execSQL(stringArgumentCaptor.capture());
        verify(jobManager).schedule(any());
        verify(eventClientRepository, timeout(6000)).fetchEventClientsByEventTypes(
                Arrays.asList(FamilyConstants.EventType.FAMILY_REGISTRATION, FamilyConstants.EventType.FAMILY_MEMBER_REGISTRATION,
                        FamilyConstants.EventType.UPDATE_FAMILY_REGISTRATION, FamilyConstants.EventType.UPDATE_FAMILY_MEMBER_REGISTRATION));
        verify(revealClientProcessor, timeout(5000)).processClient(any());
        verify(revealRepository).onUpgrade(sqLiteDatabase, 1, BuildConfig.DATABASE_VERSION);
    }
}
