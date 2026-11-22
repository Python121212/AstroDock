package io.github.python121212.astrodock.client;

import java.util.*;
import io.github.python121212.astrodock.ship.ShipParts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** アイテムごとの3D組み立てガイドとCreate風の補足ヒントを定義する。 */
public final class ShipGuide {
    public record Part(BlockPos pos, BlockState state) {}
    public record Step(String title, String detail, String tip, List<Part> parts) {}
    public record Entry(String title, List<Step> steps) {}

    private static final Map<Item, Entry> ENTRIES = new HashMap<>();

    private static boolean initialized;
    private static void init() {
        if (initialized) return;
        initialized = true;
        var core = ShipParts.CORE.get().defaultBlockState();
        var iron = Blocks.IRON_BLOCK.defaultBlockState();
        var armor = ShipParts.LAMINATED_ARMOR.get().defaultBlockState();
        var heavy = ShipParts.BLACKSHELL_ARMOR.get().defaultBlockState();
        var thruster = ShipParts.ENGINE.get().defaultBlockState();
        var helm = ShipParts.HELM.get().defaultBlockState();
        var console = ShipParts.CONSOLE.get().defaultBlockState();
        var plant = ShipParts.PLANT.get().defaultBlockState();
        var drive = ShipParts.DRIVE.get().defaultBlockState();
        var gen = ShipParts.GENERATOR.get().defaultBlockState();
        var cooler = ShipParts.COOLER.get().defaultBlockState();
        var grav = ShipParts.GRAVITY_CONTROLLER.get().defaultBlockState();
        var door = ShipParts.AIRLOCK.get().defaultBlockState();
        var hatch = ShipParts.HATCH.get().defaultBlockState();
        var cannon = ShipParts.CANNON.get().defaultBlockState();

        var hullSteps = List.of(
            new Step("1. 密閉外装を組む",
                "鉄ブロックや積層装甲で船室を囲みます。外壁の開口には艦外ドアか艦外トラップドアを使ってください。",
                "ヒント: 通常のドアは艦内の仕切り専用です。外に面すると船化できません。",
                boxShell(armor, door)),
            new Step("2. 艦船コアを配置する",
                "船体の壁面に艦船コアを1つ置き、右クリックで「範囲を確認」→「船化する」を押します。",
                "ヒント: 艦船レンチで対角2点を選んで自由な形の船を組むこともできます。",
                withPart(boxShell(armor, door), new Part(new BlockPos(0, 1, -1), core))),
            new Step("3. 艦内を歩いて設備を追加",
                "艦外ドアを開けて中に入り、スラスター・機関・操縦席を置いてコアの「設備を反映」を押します。",
                "ヒント: 船室ではブロックの設置・破壊やチェストの収納も自由に行えます。",
                withPart(withPart(boxShell(armor, door), new Part(new BlockPos(0, 1, -1), core)), new Part(new BlockPos(0, 1, 2), thruster)))
        );
        var hullEntry = new Entry("基本の造船と船化の手順", hullSteps);
        ENTRIES.put(ShipParts.CORE.get().asItem(), hullEntry);
        ENTRIES.put(ShipParts.WRENCH.get(), hullEntry);
        ENTRIES.put(ShipParts.AIRLOCK.get().asItem(), hullEntry);
        ENTRIES.put(ShipParts.HATCH.get().asItem(), hullEntry);

        var engineSteps = List.of(
            new Step("1. 機関コアを置く",
                "艦内の機関室に機関コアを置きます。ここが自作機関の中心になります。",
                "ヒント: 機関コアは1隻に1つ、または範囲を分けた機関室ごとに認識されます。",
                List.of(new Part(BlockPos.ZERO, plant))),
            new Step("2. 出力部と発電部をつなぐ",
                "機関コアに出力部と発電部を隣接させます。最大出力は min(4×出力部, 3×発電部) で決まります。",
                "ヒント: 出力部と発電部をバランスよく増やすと高出力になります。",
                List.of(new Part(BlockPos.ZERO, plant), new Part(new BlockPos(1, 0, 0), drive), new Part(new BlockPos(-1, 0, 0), gen))),
            new Step("3. 冷却部と制御盤を添える",
                "冷却部を増やすと過熱しにくくなります。操縦席や機関制御盤から主機を起動できます。",
                "ヒント: 熱が100に達すると出力が半減します。冷却部で連続運転を延ばせます。",
                List.of(new Part(BlockPos.ZERO, plant), new Part(new BlockPos(1, 0, 0), drive),
                    new Part(new BlockPos(-1, 0, 0), gen), new Part(new BlockPos(0, 0, 1), cooler),
                    new Part(new BlockPos(0, 1, 0), console)))
        );
        var engineEntry = new Entry("自作機関室の組み立て", engineSteps);
        for (var item : List.of(ShipParts.PLANT.get().asItem(), ShipParts.DRIVE.get().asItem(),
                ShipParts.GENERATOR.get().asItem(), ShipParts.COOLER.get().asItem(), ShipParts.CONSOLE.get().asItem())) {
            ENTRIES.put(item, engineEntry);
        }

        var flightEntry = new Entry("推進・姿勢制御の組み方", List.of(
            new Step("1. スラスターと操縦席",
                "艦体にスラスターと操縦席を配置します。操縦席に座ると視点方向への旋回・加速・上昇ができます。",
                "ヒント: 艦外ドアから出て操縦席へ歩いて移動し、右クリックで着席します。",
                List.of(new Part(new BlockPos(0, 0, 1), thruster), new Part(new BlockPos(0, 0, 0), iron),
                    new Part(new BlockPos(0, 1, 0), helm))),
            new Step("2. 重力制御装置で姿勢安定",
                "重力制御装置を艦内に1つ置くと、主機の作動中はスラスターが低くても後ろへ倒れなくなります。",
                "ヒント: 既存の艦に後付けしたときは、艦船コアの「設備を反映」を押してください。",
                List.of(new Part(new BlockPos(0, 0, 1), thruster), new Part(new BlockPos(0, 0, 0), iron),
                    new Part(new BlockPos(0, 1, 0), helm), new Part(new BlockPos(1, 0, 0), grav)))
        ));
        ENTRIES.put(ShipParts.ENGINE.get().asItem(), flightEntry);
        ENTRIES.put(ShipParts.HELM.get().asItem(), flightEntry);
        ENTRIES.put(ShipParts.GRAVITY_CONTROLLER.get().asItem(), flightEntry);

        var weaponEntry = new Entry("艦載兵装の搭載", List.of(
            new Step("1. 船体の外側に配置",
                "主砲・ミサイル・魚雷発射管を船体の外側に設置し、組み立て範囲に含めます。",
                "ヒント: 主砲は発射に機関の余剰出力が必要です。魚雷は宇宙でも使用できます。",
                List.of(new Part(new BlockPos(0, 0, 0), heavy), new Part(new BlockPos(0, 1, 0), cannon)))
        ));
        ENTRIES.put(ShipParts.CANNON.get().asItem(), weaponEntry);
        ENTRIES.put(ShipParts.MISSILES.get().asItem(), weaponEntry);
        ENTRIES.put(ShipParts.TORPEDOES.get().asItem(), weaponEntry);

        var armorEntry = new Entry("鉄板と艦船装甲の使い分け", List.of(
            new Step("1. 軽い積層装甲と黒殻重装甲",
                "広い外壁には軽い積層装甲（質量3）、機関室など守りたい箇所には黒殻重装甲（質量12）を張ります。",
                "ヒント: 重い艦ほど加速が鈍くなり、大気中での降下も速くなります。",
                List.of(new Part(new BlockPos(-1, 0, 0), armor), new Part(new BlockPos(0, 0, 0), iron),
                    new Part(new BlockPos(1, 0, 0), heavy), new Part(new BlockPos(0, 1, 0), hatch)))
        ));
        ENTRIES.put(ShipParts.IRON_PLATE.get(), armorEntry);
        ENTRIES.put(ShipParts.STRATAASTEEL_PLATE.get(), armorEntry);
        ENTRIES.put(ShipParts.BLACKSHELL_PLATE.get(), armorEntry);
        ENTRIES.put(ShipParts.LAMINATED_ARMOR.get().asItem(), armorEntry);
        ENTRIES.put(ShipParts.BLACKSHELL_ARMOR.get().asItem(), armorEntry);
    }

    public static Entry get(Item item) { init(); return ENTRIES.get(item); }

    private static List<Part> boxShell(BlockState wall, BlockState door) {
        List<Part> list = new ArrayList<>();
        for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 1 && z == 0) continue;
            if (x == 1 && y == 1 && z == 0) { list.add(new Part(new BlockPos(x, y, z), door)); continue; }
            if (Math.abs(x) == 1 || y == 0 || y == 2 || Math.abs(z) == 1) list.add(new Part(new BlockPos(x, y, z), wall));
        }
        return list;
    }

    private static List<Part> withPart(List<Part> base, Part extra) {
        List<Part> list = new ArrayList<>(base);
        list.removeIf(p -> p.pos().equals(extra.pos()));
        list.add(extra);
        return list;
    }

    private ShipGuide() {}
}
