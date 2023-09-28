package com.adamk33n3r.runelite.watchdog.ui.notifications.panels;

import com.adamk33n3r.runelite.watchdog.LengthLimitFilter;
import com.adamk33n3r.runelite.watchdog.SimpleDocumentListener;
import com.adamk33n3r.runelite.watchdog.WatchdogPanel;
import com.adamk33n3r.runelite.watchdog.WatchdogPlugin;
import com.adamk33n3r.runelite.watchdog.elevenlabs.ElevenLabs;
import com.adamk33n3r.runelite.watchdog.elevenlabs.Voice;
import com.adamk33n3r.runelite.watchdog.notifications.TextToSpeech;
import com.adamk33n3r.runelite.watchdog.notifications.tts.TTSSource;
import com.adamk33n3r.runelite.watchdog.ui.FlatTextArea;
import com.adamk33n3r.runelite.watchdog.ui.notifications.VoiceChooser;
import com.adamk33n3r.runelite.watchdog.ui.notifications.VolumeSlider;
import com.adamk33n3r.runelite.watchdog.ui.panels.NotificationsPanel;
import com.adamk33n3r.runelite.watchdog.ui.panels.PanelUtils;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;

public class TextToSpeechNotificationPanel extends NotificationPanel {
    private static final ImageIcon SPEECH_ICON;
    private static final ImageIcon SPEED_ICON;

    static {
        final BufferedImage speechImg = ImageUtil.loadImageResource(NotificationPanel.class, "speech_icon.png");
        final BufferedImage speedImg = ImageUtil.loadImageResource(NotificationPanel.class, "speed_icon.png");

        SPEECH_ICON = new ImageIcon(ImageUtil.luminanceOffset(speechImg, -80));
        SPEED_ICON = new ImageIcon(ImageUtil.luminanceOffset(speedImg, -80));
    }

    public TextToSpeechNotificationPanel(TextToSpeech notification, NotificationsPanel parentPanel, Runnable onChangeListener, PanelUtils.OnRemove onRemove) {
        super(notification, parentPanel, onChangeListener, onRemove);

        this.rebuild();
    }

    private void rebuild() {
        this.settings.removeAll();

        TextToSpeech notification = (TextToSpeech) this.notification;

        if (!WatchdogPlugin.getInstance().getConfig().ttsEnabled()) {
            JLabel ttsLabel = new JLabel("<html>Enable TTS in the config to use this Notification type</html>");
            ttsLabel.setFont(new Font(ttsLabel.getFont().getFontName(), Font.ITALIC | Font.BOLD, ttsLabel.getFont().getSize()));
            this.settings.add(ttsLabel);
            JButton settingsBtn = new JButton("Open Config");
            settingsBtn.addActionListener(ev -> WatchdogPlugin.getInstance().openConfiguration());
            this.settings.add(settingsBtn);
            return;
        }

        FlatTextArea flatTextArea = new FlatTextArea("Enter your message...", true);
        flatTextArea.setText(notification.getMessage());
        ((AbstractDocument) flatTextArea.getDocument()).setDocumentFilter(new LengthLimitFilter(200));
        flatTextArea.getDocument().addDocumentListener((SimpleDocumentListener) ev -> {
            notification.setMessage(flatTextArea.getText());
        });
        flatTextArea.getTextArea().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                flatTextArea.getTextArea().selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
                onChangeListener.run();
            }
        });
        this.settings.add(flatTextArea);

        JComboBox<TTSSource> sourceSelect = PanelUtils.createSelect(TTSSource.values(), notification.getSource(), (selected) -> {
            notification.setSource(selected);
            onChangeListener.run();
            this.rebuild();
            this.revalidate();
        });
        this.settings.add(sourceSelect);

        switch (notification.getSource()) {
            case ELEVEN_LABS:
                if (WatchdogPlugin.getInstance().getConfig().elevenLabsAPIKey().isEmpty()) {
                    JLabel ttsLabel = new JLabel("<html>Add your API key in the config to use Eleven Labs</html>");
                    ttsLabel.setFont(new Font(ttsLabel.getFont().getFontName(), Font.ITALIC | Font.BOLD, ttsLabel.getFont().getSize()));
                    this.settings.add(ttsLabel);
                    JButton settingsBtn = new JButton("Open Config");
                    settingsBtn.addActionListener(ev -> WatchdogPlugin.getInstance().openConfiguration());
                    this.settings.add(settingsBtn);
                    return;
                }
                JComboBox<Voice> voiceSelect = PanelUtils.createSelect(new Voice[]{}, null, Voice::getName, (voice) -> {
                    notification.setElevenLabsVoiceId(voice.getVoiceId());
                    //Not serialized
                    notification.setElevenLabsVoice(voice);
                });

                // Kinda hacky, but it'd also be hacky to modify the createSelect method so...shrug
                ActionListener actionListener = voiceSelect.getActionListeners()[0];
                voiceSelect.removeActionListener(actionListener);

                ElevenLabs.getVoices(WatchdogPlugin.getInstance().getHttpClient(), (voices) -> {
                    SwingUtilities.invokeLater(() -> {
                        voices.getVoices().forEach((voice) -> {
                            voiceSelect.addItem(voice);
                            if (notification.getElevenLabsVoiceId() == null) {
                                if (voice.getName().equals(WatchdogPlugin.getInstance().getConfig().defaultElevenLabsVoice())) {
                                    voiceSelect.setSelectedItem(voice);
                                }
                            } else {
                                if (voice.getVoiceId().equals(notification.getElevenLabsVoiceId())) {
                                    voiceSelect.setSelectedItem(voice);
                                }
                            }
                        });

                        voiceSelect.addActionListener(actionListener);
                    });
                });
                this.settings.add(voiceSelect);
                break;
            case LEGACY:
                JSlider rateSlider = new JSlider(1, 5, notification.getRate());
                rateSlider.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
                rateSlider.addChangeListener(ev -> {
                    notification.setRate(rateSlider.getValue());
                    onChangeListener.run();
                });
                this.settings.add(PanelUtils.createIconComponent(SPEED_ICON, "The speed of the generated speech", rateSlider));

                VoiceChooser voiceChooser = new VoiceChooser(notification);
                voiceChooser.addActionListener(e -> onChangeListener.run());
                this.settings.add(PanelUtils.createIconComponent(SPEECH_ICON, "The voice to generate speech with", voiceChooser));
                break;
        }

        VolumeSlider volumeSlider = new VolumeSlider(notification);
        volumeSlider.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
        volumeSlider.addChangeListener(e -> onChangeListener.run());
        this.settings.add(PanelUtils.createIconComponent(VOLUME_ICON, "The volume to playback speech", volumeSlider));
    }
}
