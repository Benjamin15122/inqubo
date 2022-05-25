package info.nemoworks.bid.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;
import info.nemoworks.bid.model.Bid;
import info.nemoworks.bid.model.Content;
import info.nemoworks.bid.process.BidProcess;
import info.nemoworks.inqubo.Command;
import info.nemoworks.inqubo.Task;
import org.apache.commons.scxml2.model.ModelException;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Route("/bid")
public class BidView extends VerticalLayout {

    Bid bid = new Bid();

    private BeanValidationBinder<Bid> binder =  new BeanValidationBinder<Bid>(Bid.class);

    List<String> options=new ArrayList<>();

    BidProcess bidProcess;
    Task t;
    List<Command<Bid>> commands;
    String commandStr="";

    private void constructField(FormLayout formLayout,Field field, String path){
        if(field.getType().getName().equals("java.lang.String")){
            TextField textField = new TextField(field.getName());
            binder.forField(textField).bind(path+field.getName());
            formLayout.add(textField);
        }else if(field.getType().getName().equals("boolean")){
            Checkbox checkbox=new Checkbox(field.getName());
            binder.forField(checkbox).bind(path+field.getName());
            formLayout.add(checkbox);
        }else if(field.getType().getName().equals("info.nemoworks.bid.model.Content")){
            Field[] fs = field.getType().getDeclaredFields();
            for (Field f:fs) {
                constructField(formLayout,f,"content.");
            }
        }else{

        }

    }

    public void constructFormlayout(FormLayout formLayout){
        formLayout.setWidth("500px");
        formLayout.setMaxWidth("500px");
        formLayout.getStyle().set("margin", "0 auto");
        // Allow the form layout to be responsive. On device widths 0-490px we have one
        // column, then we have two. Field labels are always on top of the fields.
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("490px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP));

    }

    public void constructSelect(FormLayout formLayout){
        formLayout.removeAll();
        Set<String> set=new HashSet<>();
        commands.forEach(e->{
            set.add(e.toString());
        });
        if(set.size()!=0){
            Select<String> select = new Select<>();
            select.setItems(set);
            select.setLabel("Commands");
            select.setEmptySelectionAllowed(true);
            select.addValueChangeListener(e->{
                commandStr=e.getValue();
                showNotify(commandStr);
            });
            formLayout.setColspan(select,2);
            formLayout.add(select);
        }
    }

    public void constructEditingView(FormLayout formLayout){
        // add editing component
        // add review button : jump to reviewing ->listener: removeAll construct ReviewingView
        // add save button no listener
    }

    public void constructReviewingView(FormLayout formLayout){
        // add approve button -> listener: removeAll constructTrackingView
        // add disapprove button -> listener: removeAll constructEditingView
    }

    public void constructTrackingView(FormLayout formLayout){
        // add finalize button ->  listener: removeAll constructClosedView
        // add track button no listener
    }

    public void constructClosedView(FormLayout formLayout){
        // add String: Finalized
    }

    public BidView() throws ModelException {
        bid.setContent(new Content("",""));

        FormLayout selectFormLayout=new FormLayout();
        constructFormlayout(selectFormLayout);

        //create 按钮
        FormLayout createFormLayout=new FormLayout();
        constructFormlayout(createFormLayout);

        Button createButton=new Button("start");
        createButton.addClickListener(buttonClickEvent ->{
            try {
                bidProcess=new BidProcess(bid);
                t=bidProcess.getPendingTask();
                showNotify(t.toString());
                bid=(Bid)t.getObject();
                binder.setBean(bid);
                commands=t.getExpectedCommands();
                constructSelect(selectFormLayout);
//                showNotify(commands.toString());
                constructEditingView(createFormLayout);
            } catch (ModelException e) {
                e.printStackTrace();
            }
        });
        createFormLayout.add(createButton);
        createFormLayout.setColspan(createButton,2);
        add(createFormLayout);


        //query表单
        FormLayout queryFormLayout = new FormLayout();
        constructFormlayout(queryFormLayout);

        Field[] fields = bid.getClass().getDeclaredFields();
        for (Field field:
             fields) {
            constructField(queryFormLayout,field,"");
        }
        add(queryFormLayout);


        //command表单
        FormLayout commandFormLayout = new FormLayout();
        constructFormlayout(commandFormLayout);

        Field[] commandFields = bid.getClass().getDeclaredFields();
        for (Field field:
                commandFields) {
            constructField(commandFormLayout,field,"");
        }
        Button submitButton=new Button("submit");
        submitButton.addClickListener(buttonClickEvent ->{
            try {
                binder.writeBean(bid);
                Command command = null;
                for(Command c:commands){
                    if(c.toString().equals(commandStr)){
                        command=c;
                    }
                }
                if(command!=null){
                    t.complete((Command) command);
                    t = bidProcess.getPendingTask();
                    if(t==null) {
                        selectFormLayout.removeAll();
                    }else{
                        showNotify(t.toString());
                        bid=(Bid)t.getObject();
                        binder.setBean(bid);
                        commands=t.getExpectedCommands();
                        constructSelect(selectFormLayout);
                    }

                }
            } catch (ValidationException e) {
                e.printStackTrace();
            }
        });
        commandFormLayout.setColspan(submitButton,2);
        commandFormLayout.add(submitButton);

        add(commandFormLayout);

        add(selectFormLayout);
    }

    private void showNotify(String notice) {
        Notification notification = Notification.show(notice);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public void showSuccess(Bid bid){
        Notification notification = Notification.show(bid.toString());
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
