/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.statusbar;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.statusbar.dagger.CentralSurfacesModule;
import com.android.systemui.statusbar.notification.collection.NotifCollection;
import com.android.systemui.statusbar.notification.collection.PipelineDumpable;
import com.android.systemui.statusbar.notification.collection.PipelineDumper;
import com.android.systemui.statusbar.phone.CentralSurfaces;
import com.android.systemui.statusbar.phone.NotificationListenerWithPlugins;
import com.android.systemui.util.time.SystemClock;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

// 🔹 IMPORT CORRIGIDO
import br.edu.ufam.icomp.devtitans.hellojavalib.HelloJavaLib;

/**
 * This class handles listening to notification updates and passing them along to
 * NotificationPresenter to be displayed to the user.
 */
@SysUISingleton
@SuppressLint("OverrideAbstract")
public class NotificationListener extends NotificationListenerWithPlugins implements
        PipelineDumpable {
    private static final String TAG = "NotificationListener";
    private static final boolean DEBUG = CentralSurfaces.DEBUG;
    private static final long MAX_RANKING_DELAY_MILLIS = 500L;

    private final Context mContext;
    private final NotificationManager mNotificationManager;
    private final SystemClock mSystemClock;
    private final Executor mMainExecutor;
    private final List<NotificationHandler> mNotificationHandlers = new ArrayList<>();

    private final Deque<RankingMap> mRankingMapQueue = new ConcurrentLinkedDeque<>();
    private final Runnable mDispatchRankingUpdateRunnable = this::dispatchRankingUpdate;
    private long mSkippingRankingUpdatesSince = -1;

    // 🔹 Armazena notificações por pacote (para agrupamento)
    private final java.util.Map<String, java.util.List<StatusBarNotification>> appNotifications = new java.util.HashMap<>();

    // 🔹 VARIÁVEIS ADICIONADAS PARA IA
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor();
    private final HelloJavaLib helloJavaLib = new HelloJavaLib();
    private final java.util.Map<String, String> summaryCache = new java.util.HashMap<>();

    /**
     * Injected constructor. See {@link CentralSurfacesModule}.
     */
    @Inject
    public NotificationListener(
            Context context,
            NotificationManager notificationManager,
            SystemClock systemClock,
            @Main Executor mainExecutor,
            PluginManager pluginManager) {
        super(pluginManager);
        mContext = context;
        mNotificationManager = notificationManager;
        mSystemClock = systemClock;
        mMainExecutor = mainExecutor;
    }

    /** Registers a listener that's notified when notifications are added/removed/etc. */
    public void addNotificationHandler(NotificationHandler handler) {
        if (mNotificationHandlers.contains(handler)) {
            throw new IllegalArgumentException("Listener is already added");
        }
        mNotificationHandlers.add(handler);
    }

    @Override
    public void onListenerConnected() {
        if (DEBUG) Log.d(TAG, "onListenerConnected");
        onPluginConnected();
        final StatusBarNotification[] notifications = getActiveNotifications();
        if (notifications == null) {
            Log.w(TAG, "onListenerConnected unable to get active notifications.");
            return;
        }
        final RankingMap currentRanking = getCurrentRanking();
        mMainExecutor.execute(() -> {
            // There's currently a race condition between the calls to getActiveNotifications() and
            // getCurrentRanking(). It's possible for the ranking that we store here to not contain
            // entries for every notification in getActiveNotifications(). To prevent downstream
            // crashes, we temporarily fill in these missing rankings with stubs.
            // See b/146011844 for long-term fix
            final List<Ranking> newRankings = new ArrayList<>();
            for (StatusBarNotification sbn : notifications) {
                newRankings.add(getRankingOrTemporaryStandIn(currentRanking, sbn.getKey()));
            }
            final RankingMap completeMap = new RankingMap(newRankings.toArray(new Ranking[0]));

            for (StatusBarNotification sbn : notifications) {
                for (NotificationHandler listener : mNotificationHandlers) {
                    listener.onNotificationPosted(sbn, completeMap);
                }
            }
            for (NotificationHandler listener : mNotificationHandlers) {
                listener.onNotificationsInitialized();
            }
        });
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn,
            final RankingMap rankingMap) {
        if (DEBUG) Log.d(TAG, "onNotificationPosted: " + sbn);

        if (sbn == null) return;

        // 🔹 1. Agrupa notificações por pacote
        String pkg = sbn.getPackageName();
        synchronized (appNotifications) {
            appNotifications.computeIfAbsent(pkg, k -> new java.util.ArrayList<>()).add(sbn);
        }

        // 🔹 2. Gera ou atualiza notificação-resumo COM IA
        postSummaryNotificationWithAI(pkg);

        // 🔹 3. Continua o fluxo normal (SystemUI handlers)
        if (!onPluginNotificationPosted(sbn, rankingMap)) {
            mMainExecutor.execute(() -> {
                for (NotificationHandler handler : mNotificationHandlers) {
                    handler.onNotificationPosted(sbn, rankingMap);
                }
            });
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap,
            int reason) {
        if (DEBUG) Log.d(TAG, "onNotificationRemoved: " + sbn + " reason: " + reason);
        if (sbn != null && !onPluginNotificationRemoved(sbn, rankingMap)) {
            mMainExecutor.execute(() -> {
                for (NotificationHandler handler : mNotificationHandlers) {
                    handler.onNotificationRemoved(sbn, rankingMap, reason);
                }
            });
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
        onNotificationRemoved(sbn, rankingMap, NotifCollection.REASON_UNKNOWN);
    }

    @Override
    public void onNotificationRankingUpdate(final RankingMap rankingMap) {
        if (DEBUG) Log.d(TAG, "onRankingUpdate");
        if (rankingMap != null) {
            // Add the ranking to the queue, then run dispatchRankingUpdate() on the main thread
            RankingMap r = onPluginRankingUpdate(rankingMap);
            mRankingMapQueue.addLast(r);
            // Maintaining our own queue and always posting the runnable allows us to guarantee the
            //  relative ordering of all events which are dispatched, which is important so that the
            //  RankingMap always has exactly the same elements that are current, per add/remove
            //  events.
            mMainExecutor.execute(mDispatchRankingUpdateRunnable);
        }
    }

    // 🔹 MÉTODO NOVO: Processa resumo com IA
    private void postSummaryNotificationWithAI(String pkg) {
        if (pkg.equals("com.android.systemui")) return;
        
        java.util.List<StatusBarNotification> list;
        synchronized (appNotifications) {
            list = new java.util.ArrayList<>(appNotifications.get(pkg));
        }

        if (list == null || list.isEmpty()) return;

        // Se tem menos de 3 notificações, não vale a pena processar com IA
        if (list.size() < 3) {
            postSimpleSummaryNotification(pkg, list);
            return;
        }

        // 🔸 Concatena os textos para enviar à IA
        StringBuilder summaryText = new StringBuilder();
        for (StatusBarNotification n : list) {
            CharSequence text = n.getNotification().extras
                .getCharSequence(android.app.Notification.EXTRA_TEXT);
            CharSequence title = n.getNotification().extras
                .getCharSequence(android.app.Notification.EXTRA_TITLE);
            
            if (title != null) summaryText.append(title).append(": ");
            if (text != null) summaryText.append(text).append("\n");
        }

        String textoOriginal = summaryText.toString();
        
        // 🔸 Mostra notificação "processando..." temporária
        postProcessingNotification(pkg, list.size());

        // 🔸 Processa com IA em background para não travar a UI
        aiExecutor.execute(() -> {
            String aiSummary = summaryCache.get(pkg);
            
            // Se não tem cache ou texto mudou, reprocessa
            if (aiSummary == null) {
                Log.d(TAG, "Processando resumo IA para " + pkg);
                aiSummary = helloJavaLib.computePiValue(textoOriginal);
                summaryCache.put(pkg, aiSummary);
            }

            // Posta a notificação final com o resumo
            final String finalSummary = aiSummary;
            mMainExecutor.execute(() -> 
                postFinalSummaryNotification(pkg, list.size(), finalSummary)
            );
        });
    }

    // 🔹 MÉTODO NOVO: Notificação temporária enquanto processa
    private void postProcessingNotification(String pkg, int count) {
        android.app.Notification notification = new android.app.Notification.Builder(
                mContext, "systemui_summary_channel")
                .setContentTitle(pkg + " (" + count + " notificações)")
                .setContentText("🤖 Gerando resumo inteligente...")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setGroup(pkg)
                .setGroupSummary(true)
                .setOngoing(true)  // Não pode ser fechada ainda
                .build();

        try {
            android.app.NotificationManager nm = mContext
                .getSystemService(android.app.NotificationManager.class);
            ensureChannel(nm);
            nm.notify(pkg.hashCode(), notification);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao postar notificação de processamento", e);
        }
    }

    // 🔹 MÉTODO NOVO: Notificação final com resumo da IA
    private void postFinalSummaryNotification(String pkg, int count, String aiSummary) {
        android.app.Notification notification = new android.app.Notification.Builder(
                mContext, "systemui_summary_channel")
                .setContentTitle(pkg + " (" + count + " notificações)")
                .setContentText("📝 " + aiSummary)
                .setStyle(new android.app.Notification.BigTextStyle()
                    .bigText("🤖 Resumo Inteligente:\n\n" + aiSummary))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setGroup(pkg)
                .setGroupSummary(true)
                .setOngoing(false)
                .build();

        try {
            android.app.NotificationManager nm = mContext
                .getSystemService(android.app.NotificationManager.class);
            ensureChannel(nm);
            nm.notify(pkg.hashCode(), notification);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao postar notificação resumida", e);
        }
    }

    // 🔹 MÉTODO NOVO: Resumo simples (sem IA) para poucas notificações
    private void postSimpleSummaryNotification(String pkg, 
            java.util.List<StatusBarNotification> list) {
        StringBuilder text = new StringBuilder();
        for (StatusBarNotification n : list) {
            CharSequence t = n.getNotification().extras
                .getCharSequence(android.app.Notification.EXTRA_TEXT);
            if (t != null) text.append("• ").append(t).append("\n");
        }

        android.app.Notification notification = new android.app.Notification.Builder(
                mContext, "systemui_summary_channel")
                .setContentTitle(pkg + " (" + list.size() + " notificações)")
                .setStyle(new android.app.Notification.BigTextStyle()
                    .bigText(text.toString()))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setGroup(pkg)
                .setGroupSummary(true)
                .build();

        try {
            android.app.NotificationManager nm = mContext
                .getSystemService(android.app.NotificationManager.class);
            ensureChannel(nm);
            nm.notify(pkg.hashCode(), notification);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao postar notificação simples", e);
        }
    }

    // 🔹 MÉTODO NOVO: Garante que o canal existe
    private void ensureChannel(android.app.NotificationManager nm) {
        if (nm.getNotificationChannel("systemui_summary_channel") == null) {
            nm.createNotificationChannel(new android.app.NotificationChannel(
                    "systemui_summary_channel",
                    "Resumos Inteligentes",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
            ));
        }
    }

    /**
     * This method is (and must be) the sole consumer of the RankingMap queue.  After pulling an
     * object off the queue, it checks if the queue is empty, and only dispatches the ranking update
     * if the queue is still empty.
     */
    private void dispatchRankingUpdate() {
        if (DEBUG) Log.d(TAG, "dispatchRankingUpdate");
        RankingMap r = mRankingMapQueue.pollFirst();
        if (r == null) {
            Log.wtf(TAG, "mRankingMapQueue was empty!");
        }
        if (!mRankingMapQueue.isEmpty()) {
            final long now = mSystemClock.elapsedRealtime();
            if (mSkippingRankingUpdatesSince == -1) {
                mSkippingRankingUpdatesSince = now;
            }
            final long timeSkippingRankingUpdates = now - mSkippingRankingUpdatesSince;
            if (timeSkippingRankingUpdates < MAX_RANKING_DELAY_MILLIS) {
                if (DEBUG) {
                    Log.d(TAG, "Skipping dispatch of onNotificationRankingUpdate() -- "
                            + mRankingMapQueue.size() + " more updates already in the queue.");
                }
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "Proceeding with dispatch of onNotificationRankingUpdate() -- "
                        + mRankingMapQueue.size() + " more updates already in the queue.");
            }
        }
        mSkippingRankingUpdatesSince = -1;
        for (NotificationHandler handler : mNotificationHandlers) {
            handler.onNotificationRankingUpdate(r);
        }
    }

    @Override
    public void onNotificationChannelModified(
            String pkgName, UserHandle user, NotificationChannel channel, int modificationType) {
        if (DEBUG) Log.d(TAG, "onNotificationChannelModified");
        if (!onPluginNotificationChannelModified(pkgName, user, channel, modificationType)) {
            mMainExecutor.execute(() -> {
                for (NotificationHandler handler : mNotificationHandlers) {
                    handler.onNotificationChannelModified(pkgName, user, channel, modificationType);
                }
            });
        }
    }

    public final void unsnoozeNotification(@NonNull String key) {
        if (!isBound()) return;
        try {
            getNotificationInterface().unsnoozeNotificationFromSystemListener(mWrapper, key);
        } catch (android.os.RemoteException ex) {
            Log.v(TAG, "Unable to contact notification manager", ex);
        }
    }

    public void registerAsSystemService() {
        try {
            registerAsSystemService(mContext,
                    new ComponentName(mContext.getPackageName(), getClass().getCanonicalName()),
                    UserHandle.USER_ALL);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to register notification listener", e);
        }
    }

    @Override
    public void dumpPipeline(@NonNull PipelineDumper d) {
        d.dump("notificationHandlers", mNotificationHandlers);
    }

    private static Ranking getRankingOrTemporaryStandIn(RankingMap rankingMap, String key) {
        Ranking ranking = new Ranking();
        if (!rankingMap.getRanking(key, ranking)) {
            ranking.populate(
                    key,
                    /* rank= */ 0,
                    /* matchesInterruptionFilter= */ false,
                    /* visibilityOverride= */ 0,
                    /* suppressedVisualEffects= */ 0,
                    /* importance= */ 0,
                    /* explanation= */ null,
                    /* overrideGroupKey= */ null,
                    /* channel= */ null,
                    /* overridePeople= */ new ArrayList<>(),
                    /* snoozeCriteria= */ new ArrayList<>(),
                    /* showBadge= */ false,
                    /* userSentiment= */ 0,
                    /* hidden= */ false,
                    /* lastAudiblyAlertedMs= */ 0,
                    /* noisy= */ false,
                    /* smartActions= */ new ArrayList<>(),
                    /* smartReplies= */ new ArrayList<>(),
                    /* canBubble= */ false,
                    /* isTextChanged= */ false,
                    /* isConversation= */ false,
                    /* shortcutInfo= */ null,
                    /* rankingAdjustment= */ 0,
                    /* isBubble= */ false,
                    /* proposedImportance= */ 0,
                    /* sensitiveContent= */ false
            );
        }
        return ranking;
    }
    // Adicione este método para compatibilidade
    public void addNotificationSettingsListener(NotificationSettingsListener listener) {
        // Método vazio para manter compatibilidade
    }
    
    @Deprecated
    public interface NotificationSettingsListener {

        default void onStatusBarIconsBehaviorChanged(boolean hideSilentStatusIcons) { }
    }

    /** Interface for listening to add/remove events that we receive from NotificationManager. */
    public interface NotificationHandler {
        void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap);
        void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap);
        void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason);
        void onNotificationRankingUpdate(RankingMap rankingMap);

        /** Called after a notification channel is modified. */
        default void onNotificationChannelModified(
                String pkgName,
                UserHandle user,
                NotificationChannel channel,
                int modificationType) {
        }

        /**
         * Called after the listener has connected to NoMan and posted any current notifications.
         */
        void onNotificationsInitialized();
    }
}