package org.saintqd.asurelib.managers;

import net.kyori.adventure.key.Key;
import org.saintqd.asurelib.AsureLib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Менеджер характеристик.
 * <p>
 * Характеристика - числовая переменная, заданная для сущности, которая задаёт бонус или штраф к определённому виду
 * действий. Например, бонус к характеристике урона повысит урон, наносимый сущностью, а штраф к характеристике здоровья
 * может снизить максимальное значение здоровья сущности.
 *
 */
public class StatManager {

    public static final StatManager INSTANCE = new StatManager();

    private static final Key UNDEFINED_SOURCE_KEY = Key.key(AsureLib.inst(), "undefined");

    private final HashMap<UUID,StatHolder> statsHolders;

    private final HashMap<Key, StatData> statsData;

    public StatManager() {
        this.statsHolders = new HashMap<>();
        this.statsData = new HashMap<>();
    }

    /**
     * Класс, содержащий информацию о носителе характеристик.
     * <p>
     * Полное значение конкретной характеристики складывается из суммы её модификаторов, имеющихся у сущности.
     */
    public static class StatHolder {

        // Ключ характеристики, Ключ источника, значение характеристики
        HashMap<Key, Set<StatModifier>> stats;

        public StatHolder() {
            this.stats = new HashMap<>();
        }

        public HashMap<Key, Set<StatModifier>> getStats() {
            return stats;
        }

    }

    /**
     * Класс, содержащий информацию о модификаторе конкретной характеристики.
     * <p>
     * Каждый модификатор имеет значение и источник, который позволяет определить, какое действие вызвало
     * присвоение модификатора. Источник полезен для обеспечения соответствия значения характеристики действительному
     * контексту.
     * <p>
     * Например, характеристика может быть изменена при экипировании игроком определённого элемента брони. В модификаторе
     * данного изменения может быть задан источник. Если в результате совершения неучтённой последовательности действий
     * игрок уже не использует броню, но модификатор характеристикивсё ещё присутствует, возможно осуществить проверку
     * по источнику, выявить лишний модификатор и удалить его.
     *
     * @param value значение модификатора характеристики.
     * @param source источник модификатора.
     */
    public record StatModifier(
            double value,
            Key source
    ) {
        public StatModifier(double value) {
            this(value, UNDEFINED_SOURCE_KEY);
        }
    }

    /**
     * Класс, задающий опциональные свойства характеристики.
     * <p>
     * Характеристика может иметь:
     * <ul>
     *     <li>минимальное значение, ниже которого значение характеристики быть не может;
     *     <li>максимальное значение, выше которого значение характеристики быть не может;
     *     <li>софт-кап-значение. Весь прирост характеристики после достижения значения софт-капа снижается вдвое.
     * </ul>
     */
    public record StatData(double minValue, double maxValue, double softCapValue) {

            public StatData(double minValue, double maxValue) {
                this(minValue, maxValue, maxValue);
            }

    }

    public void registerStatData(Key statKey, StatData statData) {
        statsData.put(statKey,statData);
    }

    public HashMap<UUID, StatHolder> getStatsHolders() {
        return statsHolders;
    }

    public HashMap<Key, StatData> getStatsData() {
        return statsData;
    }

    public double getStatValue(UUID uuid, Key statKey) {
        return getStatValue(uuid,statKey,true);
    }

    public double getStatValue(UUID uuid, Key statKey, boolean raw) {
        StatHolder holder = statsHolders.get(uuid);
        if (holder != null && holder.getStats().containsKey(statKey)) {
            double value = 0.0;
            for (StatModifier modifier : holder.getStats().get(statKey)) {
                value += modifier.value();
            }
            if (!raw) {
                StatData statData = statsData.get(statKey);
                if (statData != null) {
                    if (value < statData.minValue())
                        return statData.minValue();
                    if (value > statData.maxValue())
                        return statData.maxValue();
                    if (value > statData.softCapValue()) {
                        double overCapAmount = value - statData.softCapValue();
                        value = statData.softCapValue() + overCapAmount / 2.0;
                    }
                }
            }
            return value;
        }
        else return 0.0;
    }

    public void addStatModifier(UUID uuid, Key statkey, double value) {
        addStatModifier(uuid, statkey, value, UNDEFINED_SOURCE_KEY);
    }

    public void addStatModifier(UUID uuid, Key statkey, double value, Key sourcekey) {
        StatHolder holder = statsHolders.computeIfAbsent(uuid, _ -> new StatHolder());
        holder.getStats().computeIfAbsent(statkey, k -> new HashSet<>()).add(new StatModifier(value,sourcekey));
    }

    public void removeStatModifier(UUID uuid, Key statKey) {
        removeStatModifier(uuid, statKey, UNDEFINED_SOURCE_KEY);
    }

    public void removeStatModifier(UUID uuid, Key statKey, Key sourceKey) {
        StatHolder holder = statsHolders.get(uuid);
        if (holder == null)
            return;
        Set<StatModifier> modifiers = holder.getStats().get(statKey);
        if (modifiers == null)
            return;
        modifiers.removeIf(modifier -> modifier.source.equals(sourceKey));
    }

    public void clearStatsByNamespace(UUID uuid, String... namespaces) {
        Set<String> namespaceSet = Set.of(namespaces);
        StatHolder holder = statsHolders.get(uuid);
        if (holder == null)
            return;
        holder.getStats().keySet().removeIf(statKey -> namespaceSet.contains(statKey.namespace()));
    }
}
