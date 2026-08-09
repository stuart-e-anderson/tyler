import java.awt.*;

public class ServerSaveOrLoadDialog extends Frame
{
    Frame frame = this;
    TextField loginTextField;
    TextField password0TextField;
    TextField password1TextField;
    Label listLabel;
    java.awt.List list;
    Label filenameLabel;
    TextComponent filenameTextField; // TextComponent instead of TextField for Java 1.0 in which there is no TextField.setText()
    Button loadButton, saveButton, deleteButton, updateButton, closeButton;

    ServerSaveOrLoadDialog()
    {
        //
        // "Login:" textfield
        // "Password:" textfield 
        // "Password again:" textfield
        //
        // "Files" selector (click once sends to textfield, twice does nothing)
        // "Enter file name:" textfield
        //
        // [Load]   [Save]   [Delete]   [Update File List]   [Close]
        //


        //
        // Functionality...
        //
        loginTextField = new TextField_("", 20) {
            public boolean keyDown(Event e, int c)
            {
                //System.out.println("in loginTextField keyDown '"+(char)c+"'("+(int)c+"), text = \""+getText()+"\"");
                if (list.countItems() >= 1) // clearing an empty list gives error in Netscape Navigator Gold 3.04
                    list.clear();
                return super.keyDown(e, c);
            }
            public boolean keyUp(Event e, int c)
            {
                //System.out.println("in loginTextField keyUp '"+(char)c+"'("+(int)c+"), text = \""+getText()+"\"");
                updateEnableds();
                return super.keyUp(e, c);
            }
        };
        password0TextField = new TextField_("", 20) {
            public boolean keyDown(Event e, int c)
            {
                //System.out.println("in password0TextField keyDown '"+(char)c+"'("+(int)c+"), text = \""+getText()+"\"");
                if (list.countItems() >= 1) // clearing an empty list gives error in Netscape Navigator Gold 3.04
                    list.clear();
                return super.keyDown(e, c);
            }
            public boolean action(Event e, Object what)
            {
                //System.out.println("in password0TextField action what=\""+what+"\", text = \""+getText()+"\"");
                String p0 = password0TextField.getText();
                if (p0.length() >= 1
                 && p0.equals(password1TextField.getText())
                 && loginTextField.getText().length() >= 1)
                {
                    updateFileList();
                }
                return super.action(e, what);
            }
            public boolean keyUp(Event e, int c)
            {
                //System.out.println("in password0TextField keyUp '"+(char)c+"'("+(int)c+"), text = \""+getText()+"\"");
                updateEnableds();
                return super.keyUp(e, c);
            }
        };
            password0TextField.setEchoCharacter('*');
        password1TextField = new TextField_("", 20) {
            public boolean keyDown(Event e, int c)
            {
                //System.out.println("in password1TextField keyDown '"+(char)c+"'("+(int)c+"), text = \""+getText()+"\"");
                if (list.countItems() >= 1) // clearing an empty list gives error in Netscape Navigator Gold 3.04
                    list.clear();
                return super.keyDown(e, c);
            }
            public boolean action(Event e, Object what)
            {
                //System.out.println("in password1TextField action what=\""+what+"\", text = \""+getText()+"\"");
                String p0 = password0TextField.getText();
                if (p0.length() >= 1
                 && p0.equals(password1TextField.getText())
                 && loginTextField.getText().length() >= 1)
                {
                    updateFileList();
                }
                return super.action(e, what);
            }
            public boolean keyUp(Event e, int c)
            {
                //System.out.println("in password1TextField keyUp '"+(char)c+"'("+(int)c+"), text = \""+getText()+"\"");
                updateEnableds();
                return super.keyUp(e, c);
            }
        };
            password1TextField.setEchoCharacter('*');
        listLabel = new Label("Files");
        list = new List_(4, false) {
            public boolean handleEvent(Event e)
            {
                //System.out.println("list handleEvent: "+e.id);
                // Just assume it's a relevant event...
                String item = list.getSelectedItem();
                if (item != null)
                {
                    filenameTextField.setText(item);
                    updateEnableds();
                }
                return true; // consume
            }
        };
        filenameLabel = new Label("Enter file name:");
        filenameTextField = new TextField_("", 20) {
            public boolean action(Event e, Object what)
            {
                //System.out.println("in filenameTextField action, what="+what);
                // only do this on enter, since it can be time consuming if there are lots of files.
                selectListItemMatchingText();
                return super.action(e, what);
            }
            public boolean keyUp(Event e, int c)
            {
                //System.out.println("in filenameTextField keyUp");
                updateEnableds();
                return super.keyUp(e, c);
            }
        };
        loadButton = new Button_("Load") {
            public boolean action(Event e, Object what)
            {
                int savedCursor = frame.getCursorType();
                frame.setCursor(Frame.WAIT_CURSOR);
                load(loginTextField.getText(),
                     password0TextField.getText(),
                     filenameTextField.getText());
                frame.setCursor(savedCursor);

                selectListItemMatchingText();
                return true;
            }
        };
        saveButton = new Button_("Save") {
            public boolean action(Event e, Object what)
            {
                int savedCursor = frame.getCursorType();
                frame.setCursor(Frame.WAIT_CURSOR);

                save(loginTextField.getText(),
                     password0TextField.getText(),
                     filenameTextField.getText());
                // XXX would be better to do both in a single request
                String fileNames[] = getList(loginTextField.getText(),
                                             password0TextField.getText());
                frame.setCursor(savedCursor);
                if (list.countItems() >= 1) // clearing an empty list gives error in Netscape Navigator Gold 3.04
                    list.clear();
                for (int i = 0; i < fileNames.length; ++i)
                {
                    list.addItem(fileNames[i]);
                }

                selectListItemMatchingText();

                updateEnableds();
                frame.pack();
                return true;
            }
        };
        deleteButton = new Button_("Delete"); // non-functional
        updateButton = new Button_("Update File List") {
            public boolean action(Event e, Object what)
            {
                updateFileList();
                return true;
            }
        };
        closeButton = new Button_("Close") {
            public boolean action(Event e, Object what)
            {
                frame.dispose();
                return true;
            }
        };

        updateEnableds();


        //
        // Layout...
        //

        frame.setTitle("Tyler Save/Load from web server");

        frame.setLayout(new GridBagLayout());

        GridBagConstraints continueRow = new GridBagConstraints();
        {
            continueRow.fill = GridBagConstraints.BOTH;
            continueRow.weightx = 1.; // everything stretches horizontally
        }
        GridBagConstraints endRow = new GridBagConstraints();
        {
            endRow.gridwidth = GridBagConstraints.REMAINDER;
            endRow.fill = GridBagConstraints.BOTH;
            endRow.weightx = 1.; // everything stretches horizontally
        }

        Add(new Label("Login: "), continueRow);
        Add(loginTextField, endRow);
        Add(new Label("Password: "), continueRow);
        Add(password0TextField, endRow);
        Add(new Label("Again: "), continueRow);
        Add(password1TextField, endRow);
        Add(listLabel, endRow);

            endRow.weighty = 1.; // the list is the only thing that stretches vertically
        Add(list, endRow);
            endRow.weighty = 0.; // back to default
        Add(filenameLabel, endRow);
        Add(filenameTextField, endRow);

        Add(loadButton, continueRow);
        Add(saveButton, continueRow);
        Add(deleteButton, continueRow);
        Add(updateButton, continueRow);
        Add(closeButton, continueRow);

            {
                // XXX list looks weird unless we do this...
                list.addItem("a");
                list.addItem("b");
                list.addItem("c");
                list.addItem("d");
                list.addItem("e");
            }
        frame.pack();
            {
                // XXX and this...
                list.clear();
            }

        frame.show();
    } // ServerSaveOrLoadDialog ctor

    private void updateEnableds()
    {
        String p0 = password0TextField.getText();
        if (p0.length() >= 1
         && p0.equals(password1TextField.getText())
         && loginTextField.getText().length() >= 1)
        {
            if (list.countItems() >= 1)
            {
                listLabel.enable();
                list.enable();
            }
            else
            {
                listLabel.disable();
                list.disable();
            }
            filenameLabel.enable();
            filenameTextField.enable();
            if (filenameTextField.getText().length() >= 1)
            {
                loadButton.enable();
                saveButton.enable();
            }
            else
            {
                loadButton.disable();
                saveButton.disable();
            }
            deleteButton.disable(); // not functional
            updateButton.enable();
        }
        else
        {
            listLabel.disable();
            list.disable();
            filenameLabel.disable();
            filenameTextField.disable();
            loadButton.disable();
            saveButton.disable();
            deleteButton.disable();
            updateButton.disable();
        }
    } // updateEnableds

    private void updateFileList()
    {
        int savedCursor = frame.getCursorType();
        frame.setCursor(Frame.WAIT_CURSOR);

        String fileNames[] = getList(loginTextField.getText(),
                                     password0TextField.getText());
        frame.setCursor(savedCursor);

        if (list.countItems() >= 1) // clearing an empty list gives error in Netscape Navigator Gold 3.04
            list.clear();
        for (int i = 0; i < fileNames.length; ++i)
            list.addItem(fileNames[i]);

        selectListItemMatchingText();

        updateEnableds();
        frame.pack();
    } // updateFileList

    private void selectListItemMatchingText()
    {
        String fileName = filenameTextField.getText();

        list.deselect(list.getSelectedIndex());
        // getItems() not in Java 1.0...
        int n = list.countItems();
        for (int i = 0; i < n; ++i)
            if (list.getItem(i).equals(fileName))
            {
                list.select(i);
                list.makeVisible(i);
            }
    } // selectListItemMatchingText();

    // Container.add(Component, Object) doesn't exist in Java 1.0...
    public void Add(Component component, Object constraints)
    {
        //System.out.println("in Add");
        ((GridBagLayout)getLayout()).setConstraints(component, (GridBagConstraints)constraints);
        add(component);
    }

    // Overriding Component's...
    public boolean handleEvent(java.awt.Event event)
    {
        switch(event.id)
        {
            case java.awt.Event.WINDOW_DESTROY:
                //System.out.println("HA! disposing that darn window");
                dispose();
                return true;
        }
        return super.handleEvent(event);
    }

    //
    // Subclasses should override the following...
    // XXXhatch- is there a way to enforce this at compile time (like an interface)?
    //
        protected boolean load(String login, String password, String name)
        {
            throw new RuntimeException("pure virtual ServerSaveOrLoadDialog.load called");
            //return false;
        }
        protected boolean save(String login, String password, String name)
        {
            throw new RuntimeException("pure virtual ServerSaveOrLoadDialog.save called");
            //return false;
        }
        protected String[] getList(String login, String password)
        {
            throw new RuntimeException("pure virtual ServerSaveOrLoadDialog.getList called");
            //return new String[0];
        }

    //
    // Trivial wrapper classes to get around strange Jikes error when
    // anonymously subclassing:
    // *** Error: A constructor associated with this anonymous type does not throw the exception "java/awt/HeadlessException" thrown by its super type, "java/awt/Checkbox".
    // XXX duplicated in two files-- can we put these in their own file somehow?
    //
        private static class Checkbox_ extends java.awt.Checkbox {
            Checkbox_(String s) { super(s); }
            Checkbox_(String s, java.awt.CheckboxGroup cbg, boolean state) { super(s, cbg, state); }
        }
        private static class TextField_ extends java.awt.TextField {
            TextField_(int cols) { super(cols); }
            TextField_(String s, int cols) { super(s, cols); }
        }
        private static class Button_ extends java.awt.Button {
            Button_(String s) { super(s); }
        }
        private static class List_ extends java.awt.List {
            List_(int rows, boolean multipleMode) { super(rows, multipleMode); }
        }
        private static class Frame_ extends java.awt.Frame {
            public Frame_() { super(); }
            public Frame_(String title) { super(title); }
        }
} // class ServerSaveOrLoadDialog


