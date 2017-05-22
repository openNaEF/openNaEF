package tef.ui;

import tef.MVO;

import java.util.ArrayList;
import java.util.List;

public class ObjectRenderer {

    private static final List<Renderer> renderers__ = new ArrayList<Renderer>();

    public static interface Renderer {

        public Class<?> getTargetClass();

        public String render(Object object);
    }

    private ObjectRenderer() {
    }

    public static void addObjectRenderer(Renderer renderer) {
        renderers__.add(renderers__.size(), renderer);
    }

    public static String render(Object object) {
        if (object == null) {
            return null;
        }

        Renderer renderer = getObjectRenderer(object);
        if (renderer != null) {
            String result = renderer.render(object);
            if (result != null) {
                return result;
            }
        }

        if (object instanceof MVO) {
            return "&" + ((MVO) object).getMvoId().getLocalStringExpression();
        }

        return object.toString();
    }

    private static Renderer getObjectRenderer(Object object) {
        Renderer adaptiveRenderer = null;
        for (Renderer renderer : renderers__) {
            if (renderer.getTargetClass().isAssignableFrom(object.getClass())
                    && (adaptiveRenderer == null
                    || (adaptiveRenderer.getTargetClass()
                    .isAssignableFrom(renderer.getTargetClass())))) {
                adaptiveRenderer = renderer;
            }
        }
        return adaptiveRenderer;
    }
}
