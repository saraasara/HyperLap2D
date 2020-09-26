package games.rednblack.editor.view.ui.dialog;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.MenuItem;
import com.kotcrab.vis.ui.widget.PopupMenu;
import games.rednblack.editor.graph.*;
import games.rednblack.editor.graph.actions.config.*;
import games.rednblack.editor.graph.actions.config.value.*;
import games.rednblack.editor.graph.actions.producer.ArrayActionBoxProducer;
import games.rednblack.editor.graph.actions.producer.ValueInterpolationBoxProducer;
import games.rednblack.editor.graph.data.Graph;
import games.rednblack.editor.graph.data.GraphConnection;
import games.rednblack.editor.graph.data.GraphValidator;
import games.rednblack.editor.graph.producer.GraphBoxProducer;
import games.rednblack.editor.graph.actions.ActionFieldType;
import games.rednblack.editor.graph.producer.GraphBoxProducerImpl;
import games.rednblack.editor.graph.producer.value.ValueBooleanBoxProducer;
import games.rednblack.editor.graph.producer.value.ValueColorBoxProducer;
import games.rednblack.editor.graph.producer.value.ValueFloatBoxProducer;
import games.rednblack.editor.graph.producer.value.ValueVector2BoxProducer;
import games.rednblack.editor.graph.property.PropertyBox;
import games.rednblack.editor.view.stage.Sandbox;
import games.rednblack.h2d.common.H2DDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class NodeEditorDialog extends H2DDialog implements Graph<GraphBox<ActionFieldType>, GraphConnection, PropertyBox<ActionFieldType>, ActionFieldType> {

    private GraphValidator<GraphBox<ActionFieldType>, GraphConnection, PropertyBox<ActionFieldType>, ActionFieldType> graphValidator = new GraphValidator<>();
    private final GraphContainer<ActionFieldType> graphContainer;

    private final Set<GraphBoxProducer<ActionFieldType>> graphBoxProducers = new LinkedHashSet<>();

    public NodeEditorDialog() {
        super("Node Editor");

        addCloseButton();
        setResizable(true);

        graphBoxProducers.add(new ValueColorBoxProducer<>(new ValueColorNodeConfiguration()));
        graphBoxProducers.add(new ValueFloatBoxProducer<>(new ValueFloatNodeConfiguration()));
        graphBoxProducers.add(new ValueBooleanBoxProducer<>(new ValueBooleanNodeConfiguration()));
        graphBoxProducers.add(new ValueVector2BoxProducer<>(new ValueVector2NodeConfiguration()));
        graphBoxProducers.add(new ValueInterpolationBoxProducer(new ValueInterpolationNodeConfiguration()));

        try {
            graphBoxProducers.add(new ArrayActionBoxProducer(ParallelActionNodeConfiguration.class));
            graphBoxProducers.add(new ArrayActionBoxProducer(SequenceActionNodeConfiguration.class));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }

        graphBoxProducers.add(new GraphBoxProducerImpl<>(new MoveToActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new MoveByActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new SizeToActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new SizeByActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new ScaleToActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new ScaleByActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new RotateToActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new RotateByActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new RepeatActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new ForeverActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new FadeInActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new FadeOutActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new DelayActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new ColorActionNodeConfiguration()));
        graphBoxProducers.add(new GraphBoxProducerImpl<>(new AlphaActionNodeConfiguration()));

        graphContainer = new GraphContainer<>(VisUI.getSkin(), new PopupMenuProducer() {
            @Override
            public PopupMenu createPopupMenu(float x, float y) {
                return createGraphPopupMenu(x, y);
            }
        });
        graphContainer.setParentWindow(this);

        GraphBoxProducerImpl<ActionFieldType> entityProducer = new GraphBoxProducerImpl<>(new EntityNodeConfiguration());
        String id = UUID.randomUUID().toString().replace("-", "");
        GraphBox<ActionFieldType> graphBox = entityProducer.createDefault(VisUI.getSkin(), id);
        graphContainer.addGraphBox(graphBox, "Entity", false, 0, 0);


        GraphBoxProducerImpl<ActionFieldType> addActionProducer = new GraphBoxProducerImpl<>(new AddActionNodeConfiguration());
        graphBox = addActionProducer.createDefault(VisUI.getSkin(), "end");
        graphContainer.addGraphBox(graphBox, "Add Action", false, getPrefWidth() - 190, 0);

        getContentTable().add(graphContainer).grow();

        pack();

        addListener(
                new GraphChangedListener() {
                    @Override
                    protected boolean graphChanged(GraphChangedEvent event) {
                        if (event.isStructure())
                            updatePipelineValidation();
                        for (GraphBox<ActionFieldType> graphBox : graphContainer.getGraphBoxes()) {
                            graphBox.graphChanged(event, graphContainer.getValidationResult().hasErrors(),
                                    NodeEditorDialog.this);
                        }

                        event.stop();
                        return true;
                    }
                });

        updatePipelineValidation();
    }

    private void updatePipelineValidation() {
        graphContainer.setValidationResult(graphValidator.validateGraph(this, "end"));
    }

    private PopupMenu createGraphPopupMenu(final float popupX, final float popupY) {
        PopupMenu popupMenu = new PopupMenu();

        for (final GraphBoxProducer<ActionFieldType> producer : graphBoxProducers) {
            String menuLocation = producer.getMenuLocation();
            if (menuLocation != null) {
                String[] menuSplit = menuLocation.split("/");
                PopupMenu targetMenu = findOrCreatePopupMenu(popupMenu, menuSplit, 0);
                final String title = producer.getName();
                MenuItem valueMenuItem = new MenuItem(title);
                valueMenuItem.addListener(
                        new ClickListener(Input.Buttons.LEFT) {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                String id = UUID.randomUUID().toString().replace("-", "");
                                GraphBox<ActionFieldType> graphBox = producer.createDefault(skin, id);
                                graphContainer.addGraphBox(graphBox, title, true, popupX, popupY);
                            }
                        });
                targetMenu.addItem(valueMenuItem);
            }
        }

        return popupMenu;
    }

    private PopupMenu findOrCreatePopupMenu(PopupMenu popupMenu, String[] menuSplit, int startIndex) {
        for (Actor child : popupMenu.getChildren()) {
            MenuItem childMenuItem = (MenuItem) child;
            if (childMenuItem.getLabel().getText().toString().equals(menuSplit[startIndex]) && childMenuItem.getSubMenu() != null) {
                if (startIndex + 1 < menuSplit.length) {
                    return findOrCreatePopupMenu(childMenuItem.getSubMenu(), menuSplit, startIndex + 1);
                } else {
                    return childMenuItem.getSubMenu();
                }
            }
        }

        PopupMenu createdPopup = new PopupMenu();
        MenuItem createdMenuItem = new MenuItem(menuSplit[startIndex]);
        createdMenuItem.setSubMenu(createdPopup);
        popupMenu.addItem(createdMenuItem);
        if (startIndex + 1 < menuSplit.length) {
            return findOrCreatePopupMenu(createdPopup, menuSplit, startIndex + 1);
        } else {
            return createdPopup;
        }
    }

    @Override
    public float getPrefWidth() {
        return Sandbox.getInstance().getUIStage().getWidth() * 0.7f;
    }

    @Override
    public float getPrefHeight() {
        return Sandbox.getInstance().getUIStage().getHeight() * 0.8f;
    }

    @Override
    public GraphBox<ActionFieldType> getNodeById(String id) {
        return graphContainer.getGraphBoxById(id);
    }

    @Override
    public PropertyBox<ActionFieldType> getPropertyByName(String name) {
        return null;
    }

    @Override
    public Iterable<? extends GraphConnection> getConnections() {
        return graphContainer.getConnections();
    }

    @Override
    public Iterable<? extends GraphBox<ActionFieldType>> getNodes() {
        return graphContainer.getGraphBoxes();
    }

    @Override
    public Iterable<? extends PropertyBox<ActionFieldType>> getProperties() {
        return null;
    }
}