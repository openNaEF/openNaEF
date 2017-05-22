package pasaran.api;

import naef.mvo.Network;
import pasaran.util.TxUtil;
import tef.MVO;
import tef.TefService;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MVO の upper/lower 関係をツリー構造で返す
 */
@Path("/keseran/layer-trees")
public class LayerTrees {

    /**
     * 指定された MVO の lower をツリー構造で返す
     *
     * @param id mvo-id
     * @return lowers
     */
    @GET
    @Path("lower/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public static String lower(
            @Context HttpServletResponse response,
            @PathParam("id") String id,
            @QueryParam("time") String time,
            @QueryParam("version") String version) {
        try {
            TxUtil.beginReadTx(time, version);
            MVO mvo = TefService.instance().getMvoRegistry().get(MVO.MvoId.getInstanceByLocalId(id));

            if (mvo instanceof Network.UpperStackable) {
                Tree tree = traverse(new Tree(), (Network.UpperStackable) mvo);
                return KeseranApi.json.format(toJson(tree, Stack.lower));
            }
        } finally {
            TxUtil.closeTx();
        }
        return "{}";
    }


    public static Tree traverse(Tree result, Network.UpperStackable network) {
        if (result.networks.contains(network)) {
            return result;
        }

        Set<? extends Network.LowerStackable> uppers = network.getCurrentLowerLayers(false);
        Leaf leaf = new Leaf(network, uppers);
        result.add(leaf);

        uppers.stream()
                .filter(upper -> upper instanceof Network.UpperStackable)
                .forEach(upper -> traverse(result, (Network.UpperStackable) upper));

        return result;
    }

    public static Tree traverse(Tree result, Network.LowerStackable network) {
        if (result.networks.contains(network)) {
            return result;
        }

        Set<? extends Network.UpperStackable> uppers = network.getCurrentUpperLayers(false);
        Leaf leaf = new Leaf(network, uppers);
        result.add(leaf);

        uppers.stream()
                .filter(upper -> upper instanceof Network.LowerStackable)
                .forEach(upper -> traverse(result, (Network.LowerStackable) upper));

        return result;
    }

    public static Map toJson(Tree tree, Stack stack) {
        Leaf root = tree.root;
        if (root == null) return Collections.emptyMap();
        return toJson(tree, root, stack);
    }

    private static Map toJson(Tree tree, Leaf leaf, Stack stack) {
        Map result = new HashMap<>();
        result.put("id", ((MVO) leaf.parent).getMvoId().toString());
        result.put("type", stack.name());

        Set<? extends Network> children = leaf.children;
        if (children == null) children = Collections.emptySet();
        List leaves = children.stream()
                .map(network -> {
                    Leaf next = tree.get(network);
                    if (next == null) return null;
                    return toJson(tree, next, stack);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        result.put("children", leaves);
        return result;
    }

    public enum Stack {
        upper, lower
    }

    public static class Tree {
        public Leaf root;
        public List<Leaf> leaves = new ArrayList<>();
        public Set<Network> networks = new HashSet<>();

        public Tree add(Leaf leaf) {
            if (root == null) {
                root = leaf;
            }

            leaves.add(leaf);
            networks.add(leaf.parent);
            networks.addAll(leaf.children);
            return this;
        }

        public Leaf get(Network network) {
            if (!networks.contains(network)) return null;

            for (Leaf leaf : leaves) {
                if (leaf.parent == network) {
                    return leaf;
                }
            }
            return null;
        }
    }

    public static class Leaf {
        public Network parent;
        public Set<? extends Network> children;

        public Leaf(Network parent, Set<? extends Network> children) {
            this.parent = parent;
            this.children = children;
        }
    }
}

