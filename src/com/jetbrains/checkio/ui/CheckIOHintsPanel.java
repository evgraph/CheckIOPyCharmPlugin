package com.jetbrains.checkio.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class CheckIOHintsPanel extends JPanel implements Disposable {
  private final LinkedBlockingQueue<JComponent> hintQueue = new LinkedBlockingQueue<>();
  private final CheckIOToolWindow myCheckIOToolWindow;
  private final String myForumLink;
  private ScrollablePanel myHintsPanel;
  private final List<String> myHints;
  private final ArrayList<Editor> editors;
  private static final String HINT_PANEL_ID = "HINT";
  private static final String EMPTY_PANEL_ID = "Empty";

  public CheckIOHintsPanel(@NotNull final String forumLink,
                           @NotNull final ArrayList<String> hints,
                           @NotNull final CheckIOToolWindow toolWindow) throws IOException {
    removeAll();
    editors = new ArrayList<>();
    myCheckIOToolWindow = toolWindow;
    myForumLink = forumLink;
    myHints = hints;

    createHintPanel();
    showNewHint();
  }

  private void createHintPanel() {
    setLayout(new GridBagLayout());
    myHintsPanel = getHintsPanel(myHints);
    JPanel closeButtonPanel = createCloseLabelPanel();

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.RELATIVE;
    constraints.weightx = 1;
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.ipady = 0;
    add(closeButtonPanel, constraints);

    constraints.gridy = 1;
    constraints.weighty = 1;
    constraints.insets = new Insets(0, 0, 1, 0);
    add(new JBScrollPane(myHintsPanel), constraints);

    setPreferredSize(getPreferredSize());
  }

  private ScrollablePanel getHintsPanel(@NotNull final List<String> hints) {
    final ScrollablePanel panel = new ScrollablePanel(new GridBagLayout());

    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.RELATIVE;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1;
    constraints.gridx = 0;

    for (int i = 0; i < hints.size(); i++) {
      final String hint = hints.get(i);
      final ArrayList<String> codePartFromDocument = getCodePartFromDocument(hint);

      final JBCardLayout cardLayout = new JBCardLayout();
      final JPanel contentPanel = new JPanel(cardLayout);
      final String hintWithoutCode = deleteCodePart(hint);
      final JEditorPane editorPane = createHintTextEditorPane(hintWithoutCode);
      final JPanel hintPanel = createHintPanel(editorPane, codePartFromDocument);

      contentPanel.add(new JPanel(), EMPTY_PANEL_ID);
      contentPanel.add(hintPanel, HINT_PANEL_ID);
      constraints.gridy = i;
      panel.add(contentPanel, constraints);

      hintQueue.offer(contentPanel);
    }

    final JBCardLayout cardLayout = new JBCardLayout();
    final JPanel contentPanel = new JPanel(cardLayout);
    final JLabel continueOnForumLabel = createContinueOnForumLabel();
    final JPanel forumPanel = new JPanel(new BorderLayout());
    forumPanel.setBorder(BorderFactory.createEtchedBorder());
    forumPanel.add(continueOnForumLabel, BorderLayout.CENTER);

    contentPanel.add(new JPanel(), EMPTY_PANEL_ID);
    contentPanel.add(forumPanel, HINT_PANEL_ID);

    hintQueue.offer(contentPanel);

    constraints.gridy = hintQueue.size() - 1;
    panel.add(contentPanel, constraints);

    return panel;
  }

  private JPanel createCloseLabelPanel() {
    final JLabel closeIconLabel = new JLabel(AllIcons.Actions.Close);
    final JLabel titleLabel = new JLabel(CheckIOBundle.message("hints.panel.title"));
    final JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEtchedBorder());
    closeIconLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        myCheckIOToolWindow.hideHintPanel();
      }
    });

    panel.add(titleLabel, BorderLayout.WEST);
    panel.add(closeIconLabel, BorderLayout.EAST);
    return panel;
  }

  private JLabel createContinueOnForumLabel() {
    final JLabel moreHintsLabel = new JLabel(UIUtil.toHtml(CheckIOBundle.message("b.continue.on.forum.b")));
    moreHintsLabel.setBorder(BorderFactory.createEtchedBorder(UIUtil.getLabelBackground(), UIUtil.getLabelBackground()));
    moreHintsLabel.addMouseListener(new HintsMouseListener());
    moreHintsLabel.setHorizontalAlignment(SwingConstants.CENTER);
    moreHintsLabel.setToolTipText(CheckIOBundle.message("click.to.open.forum.on.web"));
    moreHintsLabel.setForeground(UIUtil.getListSelectionBackground());
    return moreHintsLabel;
  }

  private JPanel createHintPanel(JEditorPane editorPane, ArrayList<String> codePartFromDocument) {
    JPanel hintPanel;
    if (codePartFromDocument.isEmpty()) {
      hintPanel = createOneHintPanelWithoutCodePart(editorPane);
    }
    else {
      final String code = codePartFromDocument.get(0);
      final EditorImpl editor = createEditorForCodePart(code);
      hintPanel = createOneHintPanelWithCodePart(editorPane, (JPanel)editor.getComponent());
    }
    return hintPanel;
  }

  @NotNull
  private static JEditorPane createHintTextEditorPane(String hint) {
    final JEditorPane editorPane = new JEditorPane();
    editorPane.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
    editorPane.setEditable(false);
    editorPane.setText(hint);
    editorPane.addHyperlinkListener(e -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        final CheckIOBrowserWindow browserWindow = new CheckIOBrowserWindow();
        browserWindow.setShowProgress(true);
        browserWindow.addBackAndOpenButtons();
        browserWindow.load(e.getURL().toExternalForm());
        browserWindow.setVisible(true);
      }
    });
    return editorPane;
  }

  private static JPanel createOneHintPanelWithoutCodePart(@NotNull final JEditorPane text) {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(EditorColors.GUTTER_BACKGROUND.getDefaultColor());
    text.setBackground(EditorColors.GUTTER_BACKGROUND.getDefaultColor());
    panel.add(text, BorderLayout.CENTER);
    panel.add(Box.createVerticalStrut(10), BorderLayout.PAGE_END);
    panel.setBorder(BorderFactory.createEtchedBorder());
    return panel;
  }

  private static JPanel createOneHintPanelWithCodePart(@NotNull final JEditorPane text, @NotNull final JPanel editorComponent) {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(EditorColors.GUTTER_BACKGROUND.getDefaultColor());
    text.setBackground(EditorColors.GUTTER_BACKGROUND.getDefaultColor());
    editorComponent.setMinimumSize(editorComponent.getPreferredSize());
    panel.add(text, BorderLayout.PAGE_START);
    panel.add(editorComponent, BorderLayout.CENTER);
    panel.add(Box.createVerticalStrut(10), BorderLayout.PAGE_END);
    panel.setBorder(BorderFactory.createEtchedBorder());
    return panel;
  }

  private EditorImpl createEditorForCodePart(@NotNull final String codePart) {
    final Project project = ProjectUtil.guessCurrentProject(this);
    final EditorImpl editor =
      (EditorImpl)EditorFactory.getInstance().createEditor(new DocumentImpl(codePart), project, PythonFileType.INSTANCE, true);
    editor.setBorder(null);
    editor.setVerticalScrollbarVisible(false);
    editor.setOneLineMode(false);
    final EditorSettings editorSettings = editor.getSettings();
    editorSettings.setLineNumbersShown(false);
    editorSettings.setAdditionalPageAtBottom(false);
    editorSettings.setAdditionalColumnsCount(0);
    editorSettings.setAdditionalLinesCount(1);
    editorSettings.setRightMarginShown(false);
    editors.add(editor);
    return editor;
  }

  private static ArrayList<String> getCodePartFromDocument(@NotNull final String text) {
    final ArrayList<String> codeParts = new ArrayList<>();
    final Document parsed = Jsoup.parse(text);
    final Elements elements = parsed.select("pre").attr("class", "brush: python");
    elements.addAll(parsed.select("textarea").attr("data-code", "python"));
    codeParts.addAll(elements.stream().map(Element::text).collect(Collectors.toList()));
    return codeParts;
  }

  private static String deleteCodePart(@NotNull final String text) {
    final Document parsed = Jsoup.parse(text);
    final Elements elements = parsed.select("pre").attr("class", "brush: python");
    elements.addAll(parsed.select("textarea").attr("data-code", "python"));
    elements.forEach(Element::remove);
    return parsed.body().html();
  }

  public void showNewHint() {
    if (!hintQueue.isEmpty()) {
      final JComponent component = hintQueue.poll();
      final JBCardLayout cardLayout = (JBCardLayout)component.getLayout();
      cardLayout.swipe(component, HINT_PANEL_ID, JBCardLayout.SwipeDirection.AUTO);

      final JComponent parent = (JComponent)myHintsPanel.getParent();
      if (parent != null) {
        parent.scrollRectToVisible(component.getBounds());
      }
    }
  }

  public boolean hasUnseenHints() {
    return !hintQueue.isEmpty();
  }

  @Override
  public void dispose() {
    editors.stream().filter(editor -> !editor.isDisposed()).forEach(editor -> EditorFactory.getInstance().releaseEditor(editor));
  }


  private class HintsMouseListener extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      if (hintQueue.size() == 0) {
        BrowserUtil.browse(myForumLink);
      }
    }
  }
}
