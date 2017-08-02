package za.co.xone.controller;

import za.co.xone.entity.Messages;
import za.co.xone.controller.util.JsfUtil;
import za.co.xone.controller.util.PaginationHelper;
import za.co.xone.controller.util.XoneVerification;
import za.co.xone.session.MessagesFacade;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import javax.servlet.http.Part;

@Named("messagesController")
@SessionScoped
public class MessagesController implements Serializable {

    private File folder;
    private Part file;
    private String validateMessage = "";
    private String url = "";
    
    //creating the path where the uploaded file will be saved
    public final static String UPLOAD_DIR="resources/messages";
    
    //Creating an instance of the Messages class
    private Messages current;
    private DataModel items = null;
    
    //Injecting an EJB instance of the MessageFacade class
    @EJB
    private MessagesFacade ejbFacade;
    
    private PaginationHelper pagination;
    private int selectedItemIndex;

    public MessagesController() {
    }

    public File getFolder() {
        return folder;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    public Part getFile() {
        return file;
    }

    public void setFile(Part file) {
        this.file = file;
    }

    public String getValidateMessage() {
        return validateMessage;
    }

    public void setValidateMessage(String validateMessage) {
        this.validateMessage = validateMessage;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Messages getSelected() {
        if (current == null) {
            current = new Messages();
            selectedItemIndex = -1;
        }
        return current;
    }

    private MessagesFacade getFacade() {
        return ejbFacade;
    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(10) {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public DataModel createPageDataModel() {
                    return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(),
                        getPageFirstItem() + getPageSize()}));
                }
            };
        }
        return pagination;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        current = (Messages) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new Messages();
        selectedItemIndex = -1;
        return null;
    }
    
    //Saving the uploaded file to a specific folder
    public void saveFile() {
        try {

            String filename = file.getSubmittedFileName();
            InputStream input = file.getInputStream();
            ServletContext ctx = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
            String path = ctx.getRealPath(UPLOAD_DIR);
            folder = new File(path);
            url = path + "/" + filename;
            
            //Checking if file uploaded is as required/specified by calling 
            //the XoneVerification class fileFormatValidation() method passing 
            //the file path url
            validateMessage = XoneVerification.fileFormatValidation(url);
            if (validateMessage == null) {
                //Saving the uploaded file to a specific location
                Files.copy(input,
                        new File(folder, file.getSubmittedFileName()).toPath());
            } else {
                JsfUtil.addErrorMessage(validateMessage);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(MessagesController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MessagesController.class.getName()).log(Level.SEVERE, null, ex);

        }
    }

    public String create() {
        String number;
        List invalidPhoneNumbers = new ArrayList();

        try {
            File newFile = new File(url);
            
            Scanner scanner = new Scanner(newFile);
            
            //Reading the file content line by line and 
            //assigning the contents of each line to a String variable
            while (scanner.hasNextLine()) {
                number = scanner.nextLine();
                
                //Checking if the number read from the file is a valid number
                boolean isNumberValid = XoneVerification.numberValidation(number);
                if (true == isNumberValid) {
                    
                    //Setting the inputted variable values to their entity instances 
                    current.setNumbers(number);
                    current.setDescription(current.getDescription());
                    current.setActionDate(current.getActionDate());
                    
                    //Saving the Entity Class variables to the database
                    getFacade().create(current);

                } else {
                    //Adding the phone numbers with wrong format to an Arraylist
                    invalidPhoneNumbers.add(number);
                }

            }
            //List invalid numbers
            invalidPhoneNumbers.forEach(num -> System.out.println("Invalid Numbers: " + num));
            
            JsfUtil.addSuccessMessage("File successfully uploaded");
        } catch (FileNotFoundException e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        } catch (Exception ex) {
            Logger.getLogger(MessagesController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return prepareCreate();
    }

    public String prepareEdit() {
        current = (Messages) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("MessagesUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (Messages) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreatePagination();
        recreateModel();
        return "List";
    }

    public String destroyAndView() {
        performDestroy();
        recreateModel();
        updateCurrentItem();
        if (selectedItemIndex >= 0) {
            return "View";
        } else {
            // all items were removed - go back to list
            recreateModel();
            return "List";
        }
    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(ResourceBundle.getBundle("/Bundle").getString("MessagesDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, ResourceBundle.getBundle("/Bundle").getString("PersistenceErrorOccured"));
        }
    }

    private void updateCurrentItem() {
        int count = getFacade().count();
        if (selectedItemIndex >= count) {
            // selected index cannot be bigger than number of items:
            selectedItemIndex = count - 1;
            // go to previous page if last page disappeared:
            if (pagination.getPageFirstItem() >= count) {
                pagination.previousPage();
            }
        }
        if (selectedItemIndex >= 0) {
            current = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    public DataModel getItems() {
        if (items == null) {
            items = getPagination().createPageDataModel();
        }
        return items;
    }

    private void recreateModel() {
        items = null;
    }

    private void recreatePagination() {
        pagination = null;
    }

    public String next() {
        getPagination().nextPage();
        recreateModel();
        return "List";
    }

    public String previous() {
        getPagination().previousPage();
        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    public Messages getMessages(java.lang.Integer id) {
        return ejbFacade.find(id);
    }

    @FacesConverter(forClass = Messages.class)
    public static class MessagesControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            MessagesController controller = (MessagesController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "messagesController");
            return controller.getMessages(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            java.lang.Integer key;
            key = Integer.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Integer value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Messages) {
                Messages o = (Messages) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + Messages.class.getName());
            }
        }

    }

}
