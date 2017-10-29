// code by jph
package ch.ethz.idsc.retina.gui.gokart;

import java.awt.Dimension;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;

import ch.ethz.idsc.retina.dev.steer.SteerConfig;
import ch.ethz.idsc.retina.util.data.TensorProperties;

class PropertiesComponent extends ToolbarsComponent {
  private final Object object;
  private final Map<Field, JTextField> map = new HashMap<>();

  private void updateInstance() {
    Properties properties = new Properties();
    for (Entry<Field, JTextField> entry : map.entrySet())
      properties.setProperty(entry.getKey().getName(), entry.getValue().getText());
    TensorProperties.insert(properties, object);
  }

  public PropertiesComponent(Object object) {
    this.object = object;
    {
      JToolBar jToolBar = createRow("Actions");
      {
        JButton jButton = new JButton("udpate");
        jButton.addActionListener(e -> updateInstance());
        jToolBar.add(jButton);
      }
      {
        JButton jButton = new JButton("save");
        jButton.addActionListener(e -> {
          updateInstance();
          GokartResources.save(object);
        });
        jToolBar.add(jButton);
      }
    }
    addSeparator();
    for (Field field : object.getClass().getFields()) {
      if (TensorProperties.isTracked(field))
        try {
          JToolBar jToolBar = createRow(field.getName());
          JTextField jTextField = new JTextField();
          map.put(field, jTextField);
          jTextField.setPreferredSize(new Dimension(180, 28));
          jTextField.setText("" + field.get(object));
          jToolBar.add(jTextField);
        } catch (Exception exception) {
          // ---
        }
    }
  }

  public static void main(String[] args) {
    JFrame jFrame = new JFrame();
    PropertiesComponent pc = new PropertiesComponent(SteerConfig.GLOBAL);
    jFrame.setContentPane(pc.getScrollPane());
    jFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    jFrame.setBounds(100, 100, 400, 300);
    jFrame.setVisible(true);
  }
}
